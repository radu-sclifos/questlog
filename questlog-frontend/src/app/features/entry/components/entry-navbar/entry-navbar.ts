import { Component, output } from '@angular/core';

@Component({
  selector: 'app-entry-navbar',
  templateUrl: './entry-navbar.html',
  styleUrl: './entry-navbar.scss',
})
export class EntryNavbar {
  readonly addEntry = output<void>();
}
