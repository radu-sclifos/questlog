import { Routes } from '@angular/router';
import { authGuard } from './features/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Sign in',
    loadComponent: () =>
      import('./features/auth/pages/login/login').then((m) => m.Login),
  },
  {
    path: '',
    pathMatch: 'full',
    title: 'Your library - QuestLog',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/entry/pages/entry-list/entry-list').then((m) => m.EntryList),
  },
  { path: '**', redirectTo: '' },
];
