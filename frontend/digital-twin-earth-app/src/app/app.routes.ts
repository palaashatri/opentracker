import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./globe/globe.component').then(m => m.GlobeComponent),
  },
  {
    path: '**',
    redirectTo: '',
  },
];
