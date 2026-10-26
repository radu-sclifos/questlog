import { Injectable, OnDestroy, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
  id: number;
  message: string;
  type: ToastType;
}

@Injectable({ providedIn: 'root' })
export class ToastService implements OnDestroy {
  private readonly items = signal<Toast[]>([]);
  private readonly timers = new Map<number, ReturnType<typeof setTimeout>>();
  private nextId = 0;

  readonly toasts = this.items.asReadonly();

  // A duration of 0 keeps the toast visible until dismissed.
  show(message: string, type: ToastType = 'info', duration = 5000): number {
    const id = this.nextId++;
    this.items.update(items => [...items, { id, message, type }]);
    if (duration > 0) {
      this.timers.set(id, setTimeout(() => this.dismiss(id), duration));
    }
    return id;
  }

  success(message: string, duration = 5000): number {
    return this.show(message, 'success', duration);
  }

  error(message: string, duration = 8000): number {
    return this.show(message, 'error', duration);
  }

  warning(message: string, duration = 5000): number {
    return this.show(message, 'warning', duration);
  }

  info(message: string, duration = 5000): number {
    return this.show(message, 'info', duration);
  }

  dismiss(id: number): void {
    clearTimeout(this.timers.get(id));
    this.timers.delete(id);
    this.items.update(items => items.filter(item => item.id !== id));
  }

  clear(): void {
    this.timers.forEach(timer => clearTimeout(timer));
    this.timers.clear();
    this.items.set([]);
  }

  ngOnDestroy(): void {
    this.clear();
  }
}
