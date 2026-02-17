import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '@core/auth/auth.service';
import { User } from '@shared/models/auth.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent {

  @Output() closeProfile = new EventEmitter<void>();

  currentUser$ = this.authService.currentUser;

  showLogoutMessage: boolean = false;
  constructor(private authService: AuthService) {}

  close() {
    this.closeProfile.emit();
  }

  logout() {
    this.showLogoutMessage = true;

    // Wait 1.5 sec so user sees message
    setTimeout(() => {
      this.authService.logout();
      window.location.reload();
    }, 1500);
  }
 

}
