import { Routes } from '@angular/router';
import { homeRedirectGuard } from './features/auth/guards/home-redirect.guard';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Sign in',
    loadComponent: () =>
      import('./features/auth/pages/login/login').then((m) => m.Login),
  },
  {
    // This route only redirects, it has no component to render
    path: '',
    pathMatch: 'full',
    title: 'Your library - QuestLog',
    canActivate: [homeRedirectGuard],
    children: [],
  },
  {
    path: 'list/:username',
    title: 'Library - QuestLog',
    loadComponent: () =>
      import('./features/entry/pages/entry-list/entry-list').then((m) => m.EntryList),
  },
  { path: '**', redirectTo: '' },
];
