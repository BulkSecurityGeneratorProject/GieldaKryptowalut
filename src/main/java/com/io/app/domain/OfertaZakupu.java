package com.io.app.domain;

import javax.persistence.*;
import javax.validation.constraints.Size;


@Entity
@Table(name = "oferta_zakupu")
public class OfertaZakupu {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @Column(name = "cena")
    private double cena;
    @Size(min=10,max = 10)
    @Column(name = "data_wystawienia", length = 10)
    private String data_wystawienia;
    @Size(max = 50)
    @Column(name = "waluta",length = 50)
    private String waluta;

    @ManyToOne
    @JoinColumn(name = "jhi_user_id")
    private User user;


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        OfertaZakupu that = (OfertaZakupu)obj;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public String toString() {
        return "OfertaZakupu{" +
            "id=" + id +
            ", cena=" + cena +
            ", data_wystawienia='" + data_wystawienia + '\'' +
            ", waluta='" + waluta + '\'' +
            ", user=" + user +
            '}';
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getCena() {
        return cena;
    }

    public void setCena(double cena) {
        this.cena = cena;
    }

    public String getData_wystawienia() {
        return data_wystawienia;
    }

    public void setData_wystawienia(String data_wystawienia) {
        this.data_wystawienia = data_wystawienia;
    }

    public String getWaluta() {
        return waluta;
    }

    public void setWaluta(String waluta) {
        this.waluta = waluta;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

}
