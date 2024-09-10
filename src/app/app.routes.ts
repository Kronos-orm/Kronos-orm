import {Routes} from '@angular/router';
import {NG_DOC_ROUTING} from "@ng-doc/generated";

export const routes: Routes = [
  {
    path: '',
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'home',
      },
      {
        path: '404',
        loadComponent: () => import('./routes/exception/404.component').then((m) => m.Exception404Component)
      },
      {
        path: 'home',
        loadComponent: () => import('./routes/home/home.component').then(m => m.HomeComponent)
      },
      {
        path: 'documentation',
        loadComponent: () => import('./routes/documentation/documentation.component').then(m => m.DocumentationComponent),
        children: [
          ...NG_DOC_ROUTING
        ]
      },
    ],
  },
  {path: '**', redirectTo: '404'},
];
