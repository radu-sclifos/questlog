import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { GameService } from '../../../games/services/game.service';
import { Entry, EntryStatus } from '../../models/entry';
import { EntryService } from '../../services/entry.service';
import { EntryEditorDialog } from './entry-editor-dialog';

describe('EntryEditorDialog', () => {
  let fixture: ComponentFixture<EntryEditorDialog>;
  let root: HTMLElement;
  let dialog: HTMLDialogElement;
  let response: Subject<Entry>;
  const game = { id: 42, name: 'Zelda', coverImageId: null };
  const saved: Entry = {
    id: 7,
    game,
    platform: null,
    score: null,
    notes: null,
    status: EntryStatus.UNPLAYED,
    ownership: null,
    playtime: null,
    achievementsUnlocked: null,
    achievementsTotal: null,
  };
  let entries: { create: ReturnType<typeof vi.fn>; update: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    response = new Subject<Entry>();
    entries = { create: vi.fn(() => response), update: vi.fn(() => response) };
    TestBed.configureTestingModule({
      imports: [EntryEditorDialog],
      providers: [
        { provide: EntryService, useValue: entries },
        {
          provide: GameService,
          useValue: {
            platforms: vi.fn(() => of([])),
            searchSuggestions: vi.fn((query: string | null) =>
              of({
                items: query && query.trim().length >= 3 ? [game] : [],
                loading: false,
                error: false,
              }),
            ),
          },
        },
      ],
    });
    fixture = TestBed.createComponent(EntryEditorDialog);
    root = fixture.nativeElement;
    await fixture.whenStable();
    dialog = root.querySelector('dialog')!;
    // jsdom does not implement native modal behavior. Keep this boundary local:
    // these tests verify the editor, not browser focus, layout or the top layer.
    Object.defineProperties(dialog, {
      showModal: { configurable: true, value: vi.fn(() => (dialog.open = true)) },
      close: { configurable: true, value: vi.fn(() => (dialog.open = false)) },
    });
    fixture.componentInstance.open();
    await fixture.whenStable();
  });

  function input(name: string, value: string): void {
    const element = root.querySelector<HTMLInputElement | HTMLTextAreaElement>(
      `[formControlName="${name}"]`,
    )!;
    element.value = value;
    element.dispatchEvent(new Event('input', { bubbles: true }));
  }

  function submit(): void {
    root
      .querySelector('form')!
      .dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
  }

  function button(text: string): HTMLButtonElement {
    const match = [...root.querySelectorAll('button')].find(
      (button) => button.textContent?.trim() === text,
    );
    if (!match) throw new Error(`Button not found: ${text}`);
    return match;
  }

  function saveButton(): HTMLButtonElement {
    return root.querySelector('button[type="submit"]')!;
  }

  it('requires selecting a suggestion, and clears the selection when the title changes', async () => {
    input('title', 'Zelda');
    await fixture.whenStable();
    expect(saveButton().disabled).toBe(true);
    submit();
    expect(entries.create).not.toHaveBeenCalled();
    root.querySelector<HTMLButtonElement>('app-game-search-result')!.closest('button')!.click();
    await fixture.whenStable();
    expect(saveButton().disabled).toBe(false);
    expect(root.textContent).toContain('Game selected');
    input('title', 'Mario');
    await fixture.whenStable();
    expect(saveButton().disabled).toBe(true);
    expect(root.textContent).not.toContain('Game selected');
  });

  it.each([
    ['score', '-1'],
    ['score', '11'],
    ['score', '8.2'],
    ['playtime', '-1'],
    ['playtime', '1.5'],
    ['achievementsUnlocked', '-1'],
    ['achievementsTotal', '1.5'],
    ['notes', 'x'.repeat(2001)],
  ])('rejects invalid %s input (%#)', async (name, value) => {
    dialog.close();
    fixture.componentInstance.openForGame(game);
    await fixture.whenStable();
    button('More info').click();
    await fixture.whenStable();
    input(name, value);
    await fixture.whenStable();
    expect(saveButton().disabled).toBe(true);
    submit();
    expect(entries.create).not.toHaveBeenCalled();
  });

  it('rejects unlocked achievements above the total and shows the validation message', async () => {
    dialog.close();
    fixture.componentInstance.openForGame(game);
    await fixture.whenStable();
    button('More info').click();
    await fixture.whenStable();
    input('achievementsUnlocked', '5');
    input('achievementsTotal', '4');
    submit();
    await fixture.whenStable();
    expect(root.textContent).toContain('Unlocked achievements cannot exceed the total.');
    expect(entries.create).not.toHaveBeenCalled();
    input('achievementsTotal', '5');
    await fixture.whenStable();
    expect(saveButton().disabled).toBe(false);
  });

  it('normalizes blank notes, blocks duplicate submissions and emits the returned entry', async () => {
    dialog.close();
    fixture.componentInstance.openForGame(game);
    await fixture.whenStable();
    button('More info').click();
    await fixture.whenStable();
    input('notes', '   ');
    const emitted = vi.fn();
    fixture.componentInstance.savedEntry.subscribe(emitted);
    submit();
    submit();
    await fixture.whenStable();
    expect(entries.create).toHaveBeenCalledExactlyOnceWith({
      gameId: 42,
      platformId: null,
      score: null,
      status: null,
      ownership: null,
      playtime: null,
      achievementsUnlocked: null,
      achievementsTotal: null,
      notes: null,
    });
    expect(saveButton().disabled).toBe(true);
    expect(button('Cancel').disabled).toBe(true);
    const cancel = new Event('cancel', { cancelable: true });
    dialog.dispatchEvent(cancel);
    expect(cancel.defaultPrevented).toBe(true);
    response.next(saved);
    response.complete();
    await fixture.whenStable();
    expect(emitted).toHaveBeenCalledExactlyOnceWith(saved);
    expect(dialog.open).toBe(false);
  });

  it('keeps the dialog and entered values after a conflict, allowing retry', async () => {
    dialog.close();
    fixture.componentInstance.openForGame(game);
    await fixture.whenStable();
    input('playtime', '12');
    submit();
    response.error(new HttpErrorResponse({ status: 409 }));
    await fixture.whenStable();
    expect(root.textContent).toContain('This game is already in your library.');
    expect(dialog.open).toBe(true);
    expect(root.querySelector<HTMLInputElement>('[formControlName="playtime"]')!.value).toBe('12');
    expect(saveButton().disabled).toBe(false);
    const retry = new Subject<Entry>();
    entries.create.mockReturnValue(retry);
    submit();
    expect(entries.create).toHaveBeenCalledTimes(2);
    retry.next(saved);
    retry.complete();
    expect(dialog.open).toBe(false);
  });

  it('updates an existing entry and makes its game title read-only', async () => {
    dialog.close();
    fixture.componentInstance.open(saved);
    await fixture.whenStable();
    expect(root.querySelector<HTMLInputElement>('[formControlName="title"]')!.readOnly).toBe(true);
    input('playtime', '20');
    submit();
    expect(entries.create).not.toHaveBeenCalled();
    expect(entries.update).toHaveBeenCalledExactlyOnceWith(
      7,
      expect.objectContaining({ gameId: 42, playtime: 20 }),
    );
    response.next({ ...saved, playtime: 20 });
    response.complete();
    expect(dialog.open).toBe(false);
  });
});
