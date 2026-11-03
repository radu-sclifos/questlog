import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AuthService } from '../../../auth/services/auth.service';
import { ToastService } from '../../../../shared/toast/toast.service';
import { PublicProfile } from '../../models/public-profile';
import { avatarUrl, initials } from '../../utils/profile-image';
import { ProfileService } from '../../services/profile.service';

@Component({
  selector: 'app-profile-editor',
  imports: [ReactiveFormsModule],
  templateUrl: './profile-editor.html',
})
export class ProfileEditor {
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  private readonly profiles = inject(ProfileService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  readonly saved = output<void>();
  readonly avatarChanged = output<string | null>();

  protected readonly profile = signal<PublicProfile | null>(null);
  protected readonly revision = signal<string | null>(null);

  protected readonly saving = signal(false);
  protected readonly uploading = signal(false);
  protected readonly busy = computed(() => this.saving() || this.uploading());

  protected readonly selectedFile = signal<File | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly photoError = signal<string | null>(null);
  protected readonly imageFailed = signal(false);
  protected readonly preview = signal<string | null>(null);
  protected readonly imageUrl = computed(
    () => this.preview() ?? avatarUrl(this.profile()?.username ?? '', this.revision()),
  );
  protected readonly initials = initials;

  protected readonly form = new FormGroup({
    displayName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)],
    }),
    bio: new FormControl('', {
      nonNullable: true,
      validators: Validators.maxLength(300),
    }),
  });

  constructor() {
    this.destroyRef.onDestroy(() => this.clearPreview());
  }

  open(profile: PublicProfile): void {
    this.clearPreview();
    this.profile.set(profile);
    this.revision.set(profile.avatarRevision);
    this.form.reset({ displayName: profile.displayName, bio: profile.bio ?? '' });
    this.error.set(null);
    this.photoError.set(null);
    this.imageFailed.set(false);
    this.dialog().nativeElement.showModal();
  }

  protected close(): void {
    if (this.busy()) {
      return;
    }
    this.clearPreview();
    this.dialog().nativeElement.close();
  }

  protected choosePhoto(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';

    if (!file || this.busy()) {
      return;
    }

    this.photoError.set(null);
    if (
      !['image/png', 'image/jpeg'].includes(file.type) ||
      file.size > 2 * 1024 * 1024 ||
      !file.size
    ) {
      this.photoError.set('Choose a PNG or JPEG image up to 2 MB.');
      return;
    }
    this.clearPreview();
    this.selectedFile.set(file);
    // Create a temporary preview of the profile picture
    this.preview.set(URL.createObjectURL(file));
    this.imageFailed.set(false);
  }

  // Clear the temporary preview of the profile picture
  protected clearPreview(): void {
    const url = this.preview();
    if (url) {
      URL.revokeObjectURL(url);
    }
    this.preview.set(null);
    this.selectedFile.set(null);
  }

  protected upload(): void {
    const file = this.selectedFile();
    if (!file || this.busy()) {
      return;
    }
    this.uploading.set(true);
    this.photoError.set(null);

    this.profiles
      .uploadAvatar(file)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.uploading.set(false)),
      )
      .subscribe({
        next: ({ revision }) => {
          this.revision.set(revision);
          this.clearPreview();
          this.imageFailed.set(false);
          this.avatarChanged.emit(revision);
          this.toast.success('Profile picture updated.');
        },
        error: (error: unknown) =>
          this.photoError.set(
            error instanceof HttpErrorResponse && [400, 413].includes(error.status)
              ? 'Choose a valid PNG or JPEG up to 2 MB and 16 megapixels.'
              : 'Unable to upload your picture. Please try again.',
          ),
      });
  }

  protected removePhoto(): void {
    if (this.busy()) {
      return;
    }
    this.uploading.set(true);
    this.photoError.set(null);

    this.profiles
      .removeAvatar()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.uploading.set(false)),
      )
      .subscribe({
        next: () => {
          this.clearPreview();
          this.revision.set(null);
          this.avatarChanged.emit(null);
          this.toast.success('Profile picture removed.');
        },
        error: () => this.photoError.set('Unable to remove your picture. Please try again.'),
      });
  }

  protected save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.busy() || this.selectedFile()) {
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    const { displayName, bio } = this.form.getRawValue();

    this.auth
      .updateProfile(displayName.trim(), bio.trim() || null)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: () => {
          this.clearPreview();
          this.dialog().nativeElement.close();
          this.saved.emit();
          this.toast.success('Profile updated.');
        },
        error: () => this.error.set('Unable to save your profile. Please try again.'),
      });
  }
}
