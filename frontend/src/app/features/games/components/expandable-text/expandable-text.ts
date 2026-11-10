import {
  afterRenderEffect,
  Component,
  computed,
  ElementRef,
  input,
  linkedSignal,
  signal,
  viewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';

interface TextLink {
  name: string;
  route: (string | number)[];
}

@Component({
  selector: '[app-expandable-text]',
  imports: [RouterLink],
  templateUrl: './expandable-text.html',
})
export class ExpandableText {
  readonly label = input.required<string>();
  readonly value = input.required<string | readonly (string | TextLink)[]>();
  protected readonly values = computed(() => {
    const value = this.value();
    if (typeof value === 'string') return [value];
    return value.length ? value : ['—'];
  });
  protected readonly expanded = linkedSignal({ source: this.value, computation: () => false });
  protected readonly overflowing = signal(false);
  private readonly content = viewChild.required<ElementRef<HTMLElement>>('content');

  constructor() {
    afterRenderEffect((onCleanup) => {
      this.value();
      this.expanded();
      const element = this.content().nativeElement;

      // Measure rendered lines so the control only appears for genuinely long values
      const measure = () => {
        const lineHeight = Number.parseFloat(getComputedStyle(element).lineHeight);
        this.overflowing.set(element.scrollHeight > lineHeight * 2 + 1);
      };

      measure();
      const observer = new ResizeObserver(measure);
      observer.observe(element);
      onCleanup(() => observer.disconnect());
    });
  }

  protected toggle(): void {
    this.expanded.update((value) => !value);
  }
}
