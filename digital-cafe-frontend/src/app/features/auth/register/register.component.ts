import { Component, OnInit } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { Router, RouterModule } from "@angular/router";
import { AuthService } from "../../../core/auth/auth.service";
import { NavbarComponent } from "../../../shared/components/navbar/navbar.component";
import {
  PersonalDetails,
  AddressInfo,
  AcademicInfo,
  WorkExperience,
  RegisterRequest,
} from "../../../shared/models/auth.model";

@Component({
  selector: "app-register",
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, NavbarComponent],
  templateUrl: "./register.component.html",
  styleUrl: "./register.component.scss",
})
export class RegisterComponent implements OnInit {

  currentStep = 1;
  totalSteps = 5;
  isLoading = false;
  errorMessage = "";
  successMessage = "";

  currentYear = new Date().getFullYear(); // used in HTML

  // Step-1 Role Only
  role = "";

  personalDetails: PersonalDetails = {
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
    gender: "",
    maritalStatus: "SINGLE",
  };

  address: AddressInfo = {
    street: "",
    city: "",
    state: "",
    pincode: "",
  };

  academicInfoList: AcademicInfo[] = [
    {
      institutionName: "",
      degree: "",
      passingYear: this.currentYear,
      grade: "",
      gradeInPercentage: 0,
    },
  ];

  workExperienceList: WorkExperience[] = [];

  genderOptions = ["MALE", "FEMALE", "OTHER", "PREFER_NOT_TO_SAY"];
  maritalStatusOptions = ["SINGLE", "MARRIED", "DIVORCED", "WIDOWED"];
  roleOptions = ["CUSTOMER", "CAFE_OWNER", "CHEF", "WAITER"];

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit() {}

  // ---------------- STEP NAVIGATION ----------------

  nextStep() {
    if (this.validateCurrentStep()) this.currentStep++;
  }

  previousStep() {
    if (this.currentStep > 1) this.currentStep--;
  }

  goToStep(step: number) {
    if (this.isStepAccessible(step)) this.currentStep = step;
  }

  isStepAccessible(step: number): boolean {
    return step <= this.currentStep;
  }

  getStepIcon(step: number): string {
    if (step < this.currentStep) return "bi-check-circle-fill";
    if (step === this.currentStep) return "bi-circle-fill";
    return "bi-circle";
  }

  getStepClass(step: number): string {
    if (step < this.currentStep) return "completed";
    if (step === this.currentStep) return "active";
    return "pending";
  }

  // ---------------- VALIDATION ----------------

  validateCurrentStep(): boolean {
    switch (this.currentStep) {
      case 1: return this.role !== "";
      case 2: return !!this.personalDetails.firstName && !!this.personalDetails.email;
      case 3: return !!this.address.city;
      default: return true;
    }
  }

  // ---------------- ACADEMIC ----------------

  addAcademic() {
    this.academicInfoList.push({
      institutionName: "",
      degree: "",
      passingYear: this.currentYear,
      grade: "",
      gradeInPercentage: 0,
    });
  }

  removeAcademic(index: number) {
    if (this.academicInfoList.length > 1)
      this.academicInfoList.splice(index, 1);
  }

  // ---------------- EXPERIENCE ----------------

  addWorkExperience() {
    this.workExperienceList.push({
      startDate: "",
      endDate: "",
      currentlyWorking: false,
      companyName: "",
      designation: "",
      ctc: { amount: 0, currency: "LPA" },
      reasonForLeaving: "",
    });
  }

  removeWorkExperience(index: number) {
    this.workExperienceList.splice(index, 1);
  }

  onCurrentlyWorkingChange(index: number) {
    if (this.workExperienceList[index].currentlyWorking) {
      this.workExperienceList[index].endDate = "";
      this.workExperienceList[index].reasonForLeaving = "";
    }
  }

  // ---------------- SUBMIT ----------------

  onSubmit() {

    this.isLoading = true;

    const request: RegisterRequest = {
      role: this.role,
      personalDetails: this.personalDetails,
      address: this.address,
      academicInfoList: this.academicInfoList,
      workExperienceList: this.workExperienceList.length
        ? this.workExperienceList
        : undefined,
    };

    this.authService.register(request).subscribe({
      next: () => {
        this.successMessage = "Registration Submitted. Wait for Admin Approval.";
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = "Registration Failed";
        this.isLoading = false;
      },
    });
  }
}
