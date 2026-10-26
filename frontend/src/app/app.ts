import { Component } from '@angular/core';
import { EntryList } from './features/entry/pages/entry-list/entry-list';
import { ToastContainer } from './shared/toast/toast-container';

@Component({
  selector: 'app-root',
  imports: [EntryList, ToastContainer],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {}
