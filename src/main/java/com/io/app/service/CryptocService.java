package com.io.app.service;


import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import io.undertow.util.SameThreadExecutor;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.MemoryBlockStore;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.wallet.KeyChain;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkNotNull;

public class CryptocService {

    private static Address forwardingAddress;
    private static WalletAppKit kit;


    public Wallet LoadWallet(String username, NetworkParameters netParams){

        Wallet wallet = null;
        final File walletFile = new File("wallets/"+username+".wallet");

        try{
            wallet = new Wallet(netParams);


            if(walletFile.exists())
            {
                wallet = wallet.loadFromFile(walletFile);
                System.out.println("Wallet loaded!");
            }
            else
            {
                wallet.addKey(new ECKey());
                System.out.println("Wallet created!");
            }

            wallet.saveToFile(walletFile);

        }catch (IOException | UnreadableWalletException e ) {System.out.println("Unable to create or load wallet file.");}

        return wallet;
    }

    /* Divide to GetWalletAdress and GetWalletBalance */
    public void GetWalletInfo(String username,NetworkParameters netParams){
        System.out.println("WALLET INFO: "+LoadWallet(username,netParams));
    }

    public void SaveWallet(Wallet wallet, String username){
        final File walletFile = new File("wallets/"+username+".wallet");

        try {
            wallet.saveToFile(walletFile);
        }catch (IOException e) {
            System.out.println("Can't save wallet info");
        }
    }

    public void RefreshWallet(Wallet wallet,NetworkParameters netparams, String nickname){

        System.out.println("START REFRESH...");
        File file = new File("wallets/"+nickname+".wallet");
        BlockStore blockStore = new MemoryBlockStore(netparams);
        BlockChain chain = null;

        try{
            chain = new BlockChain(netparams, wallet, blockStore);
        }catch (BlockStoreException e) {
            System.out.println("Block store error!");
        }

        final PeerGroup peerGroup = new PeerGroup(netparams, chain);
        peerGroup.startAsync();

        wallet.addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {
            @Override
            public synchronized void onCoinsReceived(Wallet w, Transaction tx, Coin prevBalance, Coin newBalance) {
                System.out.println("\nReceived tx " + tx.getHashAsString());
                System.out.println(tx.toString());
            }
        });

        peerGroup.downloadBlockChain();
        peerGroup.stopAsync();
        try {
            wallet.saveToFile(file);
        }catch (IOException e){
            System.out.println("RefreshWallet: file not saved!");
        }
        System.out.println("\nDone Refreshing Wallet!\n");
        System.out.println(wallet.toString());

    }

    ////------------------------///

    public void Wallet(){

        // Figure out which network we should connect to. Each one gets its own set of files.
        NetworkParameters params = RegTestParams.get();
        String filePrefix = "forwarding-service-regtest";

        // Parse the address given as the first parameter.
        forwardingAddress = Address.fromBase58(params, "mySnxjrChcpmquYsxnbRtFqSuNDeyW1Cyb");


        System.out.println("Network: " + params.getId());
        System.out.println("Forwarding address: " + forwardingAddress);

        // Start up a basic app using a class that automates some boilerplate.
        kit = new WalletAppKit(params, new File("."), filePrefix);

        if (params == RegTestParams.get()) {
            // Regression test mode is designed for testing and development only, so there's no public network for it.
            // If you pick this mode, you're expected to be running a local "bitcoind -regtest" instance.
            kit.connectToLocalHost();
        }

        // Download the block chain and wait until it's done.
        kit.startAsync();
        kit.awaitRunning();

        // We want to know when we receive money.
        kit.wallet().addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {
            @Override
            public void onCoinsReceived(Wallet w, Transaction tx, Coin prevBalance, Coin newBalance) {
                // Runs in the dedicated "user thread" (see bitcoinj docs for more info on this).
                //
                // The transaction "tx" can either be pending, or included into a block (we didn't see the broadcast).
                Coin value = tx.getValueSentToMe(w);
                System.out.println("Received tx for " + value.toFriendlyString() + ": " + tx);
                System.out.println("Transaction will be forwarded after it confirms.");
                // Wait until it's made it into the block chain (may run immediately if it's already there).
                //
                // For this dummy app of course, we could just forward the unconfirmed transaction. If it were
                // to be double spent, no harm done. Wallet.allowSpendingUnconfirmedTransactions() would have to
                // be called in onSetupCompleted() above. But we don't do that here to demonstrate the more common
                // case of waiting for a block.
                Futures.addCallback(tx.getConfidence().getDepthFuture(1), new FutureCallback<TransactionConfidence>() {
                    @Override
                    public void onSuccess(TransactionConfidence result) {
                        forwardCoins(tx);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        // This kind of future can't fail, just rethrow in case something weird happens.
                        throw new RuntimeException(t);
                    }
                });
            }
        });

        Address sendToAddress = kit.wallet().currentReceiveKey().toAddress(params);
        System.out.println("Send coins to: " + sendToAddress);
        System.out.println("Waiting for coins to arrive. Press Ctrl-C to quit.");

        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ignored) {}
    }

    private static void forwardCoins(Transaction tx) {
        try {
            Coin value = tx.getValueSentToMe(kit.wallet());
            System.out.println("Forwarding " + value.toFriendlyString());
            // Now send the coins back! Send with a small fee attached to ensure rapid confirmation.
            final Coin amountToSend = value.subtract(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE);
            final Wallet.SendResult sendResult = kit.wallet().sendCoins(kit.peerGroup(), forwardingAddress, amountToSend);
            checkNotNull(sendResult);  // We should never try to send more coins than we have!
            System.out.println("Sending ...");
            // Register a callback that is invoked when the transaction has propagated across the network.
            // This shows a second style of registering ListenableFuture callbacks, it works when you don't
            // need access to the object the future returns.
            sendResult.broadcastComplete.addListener(new Runnable() {
                @Override
                public void run() {
                    // The wallet has changed now, it'll get auto saved shortly or when the app shuts down.
                    System.out.println("Sent coins onwards! Transaction hash is " + sendResult.tx.getHashAsString());
                }
            }, SameThreadExecutor.INSTANCE);
        } catch (KeyCrypterException | InsufficientMoneyException e) {
            // We don't use encrypted wallets in this example - can never happen.
            throw new RuntimeException(e);
        }
    }

}
