import { NgTemplateOutlet, ViewportScroller } from '@angular/common';
import { Component, inject, input } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-logo',
  imports: [NgTemplateOutlet],
  templateUrl: './logo.html',
  host: {
    class: 'inline-block w-64 max-w-full',
    '[style.width]': 'width()',
  },
})
export class Logo {
  private readonly router = inject(Router);
  private readonly viewport = inject(ViewportScroller);

  readonly width = input<string | null>(null);
  readonly link = input<string | null>('/');

  protected async navigate(event: MouseEvent, destination: string): Promise<void> {
    if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) {
      return;
    }

    event.preventDefault();
    await this.router.navigateByUrl(destination);
    this.viewport.scrollToPosition([0, 0], { behavior: 'instant' });
  }
}
