import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastContainer } from './shared/toast/toast-container';
import { Footer } from './shared/footer/footer';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastContainer, Footer],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {}
