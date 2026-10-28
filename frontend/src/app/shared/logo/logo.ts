import { NgTemplateOutlet } from '@angular/common';
import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-logo',
  imports: [NgTemplateOutlet, RouterLink],
  templateUrl: './logo.html',
  host: {
    class: 'inline-block w-64 max-w-full',
    '[style.width]': 'width()',
  },
})
export class Logo {
  readonly width = input<string | null>(null);
  readonly link = input<string | null>('/');
}
