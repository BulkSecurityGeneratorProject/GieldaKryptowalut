import { Routes, RouterModule, Route } from '@angular/router';

import { NgModule } from '../../../../../../node_modules/@angular/core';
import { KupnoSprzedazComponent } from './kupno-sprzedaz.component';

export const KupnoSprzedazRouter: Route = {
    path: 'kupno-sprzedaz',
    component: KupnoSprzedazComponent
};
// @NgModule({
//     imports: [RouterModule.forRoot(routes)],
//     exports: [RouterModule]
// })
// export class MojeKontoRouter { }
