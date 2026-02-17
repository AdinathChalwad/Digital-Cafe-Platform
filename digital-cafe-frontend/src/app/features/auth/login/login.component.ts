import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
} from "@angular/forms";
import { Router, RouterModule, ActivatedRoute } from "@angular/router";
import { AuthService } from "@core/auth/auth.service";
import { NotificationService } from "@core/services/notification.service";
import { NavbarComponent } from "@shared/components/navbar/navbar.component";

@Component({
  selector: "app-login",
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, NavbarComponent],
  templateUrl: "./login.component.html",
  styleUrls: ["./login.component.scss"],
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  loading = false;
  showPassword = false;
  returnUrl: string = "/";

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private notificationService: NotificationService,
  ) {}

  ngOnInit(): void {
    // Initialize login form
    this.loginForm = this.fb.group({
      email: ["", [Validators.required, Validators.email]],
      password: ["", [Validators.required, Validators.minLength(8)]],
    });

    // Get return URL if redirected from guard
    this.returnUrl = this.route.snapshot.queryParams["returnUrl"] || "/";

    // If already logged in → go directly to dashboard
    if (this.authService.isAuthenticated) {
      this.router.navigate([this.authService.getRoleDashboardRoute()]);
    }
  }

  get f() {
    return this.loginForm?.controls || {};
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
  if (!this.loginForm || this.loginForm.invalid) {
    Object.keys(this.loginForm.controls).forEach((key) => {
      this.loginForm.controls[key].markAsTouched();
    });
    return;
  }

  this.loading = true;

  this.authService.login(this.loginForm.value).subscribe({
    next: (response) => {
      this.loading = false;

      this.notificationService.success("Login successful!");

      // If customer profile incomplete → redirect to complete profile
      if (response.roles?.includes("CUSTOMER") && !response.isProfileComplete) {
        this.router.navigate(["/customer/complete-profile"]);
        return;
      }

      // ✅ ROLE BASED REDIRECT (directly from response)
      if (response.roles?.includes("ADMIN")) {
        this.router.navigate(["/admin/dashboard"]);
      } else if (response.roles?.includes("CAFE_OWNER")) {
        this.router.navigate(["/cafe-owner/dashboard"]);
      } else if (response.roles?.includes("CHEF")) {
        this.router.navigate(["/chef/dashboard"]);
      } else if (response.roles?.includes("WAITER")) {
        this.router.navigate(["/waiter/dashboard"]);
      } else {
        this.router.navigate(["/customer/dashboard"]);
      }
    },

    error: (error) => {
      this.loading = false;
      this.notificationService.error(
        error.message || "Login failed. Please check your credentials."
      );
    },
  });
}

}