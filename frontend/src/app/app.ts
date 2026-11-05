import { Component, inject } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs';
import { Navbar } from './shared/navbar/navbar';
import { ToastContainer } from './shared/toast/toast-container';
import { Footer } from './shared/footer/footer';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastContainer, Footer, Navbar],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly router = inject(Router);
  protected readonly showNavbar = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map(event => this.router.parseUrl(event.urlAfterRedirects).root.children['primary']?.segments[0]?.path !== 'login'),
    ),
    { initialValue: this.router.parseUrl(this.router.url).root.children['primary']?.segments[0]?.path !== 'login' },
  );
}
