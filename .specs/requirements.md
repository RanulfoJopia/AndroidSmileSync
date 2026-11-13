# Treatment Plan Feature Sync - Requirements

## Overview
Sync Android SmileSync treatment plan functionality with the Vue/Laravel web implementation to provide consistent features and AI-powered treatment planning across platforms.

## Background
The Vue.js web application has a comprehensive treatment planning system with structured clinical data collection, AI-generated remarks (6 sections), treatment sequencing, dental chart visualization, and a 10-category treatment checklist. The Android app currently has a simpler implementation that needs to be enhanced to match the web version's capabilities.

## Goals
1. Match the web application's treatment planning workflow and features
2. Implement structured clinical data collection
3. Add AI-powered treatment remarks generation (6 sections)
4. Enhance treatment sequencing with dependencies
5. Add treatment checklist with 10 standard categories
6. Improve UI/UX to match modern web design

## User Stories

### Clinical Data Management
**As a dentist**, I want to enter structured clinical data for a patient so that AI can generate accurate treatment plans and remarks.

**Acceptance Criteria:**
- Can open a clinical input form from treatment case detail page
- Form includes fields for:
  - Chief complaint
  - Dentist diagnosis
  - Clinical findings
  - Vital signs
  - Medical history
  - Current medications
  - Allergies
  - Other relevant clinical data
- Clinical data is saved to the backend
- Can view and edit saved clinical data
- Clinical data is associated with a dental measurement/case

### AI-Generated Treatment Remarks
**As a dentist**, I want AI to analyze clinical data and generate structured treatment remarks in 6 sections so that I have comprehensive treatment guidance.

**Acceptance Criteria:**
- Can click "Generate Remarks" button after clinical data is saved
- AI generates remarks in 6 sections:
  1. Urgent Treatment Needs
  2. Systemic Considerations
  3. Preparatory Procedures
  4. Decision Point for Major Treatment
  5. Corrective/Restorative Treatment
  6. Maintenance & Follow-up
- Each section displays with distinct visual styling
- Generated remarks are saved and persist across sessions
- Can regenerate remarks if clinical data changes

### Treatment Plan Checklist
**As a dentist**, I want a standardized treatment checklist with 10 categories so that I can systematically plan and track treatments.

**Acceptance Criteria:**
- Checklist displays 10 treatment categories:
  - A: Urgent
  - B: Periodontal
  - C: Prophylaxis
  - D: Topical Fluoride (SnF2)
  - E: Counseling in self care
  - F: Occlusion
  - G: Surgery
  - H: Restorations
  - I: Prostheses
  - J: Other (specify)
- Each item shows:
  - Category code and name
  - Planned sequence (tooth numbers/order)
  - Accomplished checkbox
  - Notes field
- Checklist integrates with dental measurement data
- Can mark items as accomplished
- Can add notes for each category

### Enhanced AI Sequencing
**As a dentist**, I want AI to generate optimized treatment sequences based on clinical dependencies so that treatments are performed in the correct order.

**Acceptance Criteria:**
- Can click "Generate Treatment Sequence" button
- AI analyzes:
  - Clinical data
  - Dental measurements
  - Treatment checklist
  - Dependencies between treatments
- Generates sequences with:
  - Step number/order
  - Treatment description
  - Procedure code
  - Phase type
  - Status
  - Dependencies on other steps
- Sequences are displayed in optimal order
- Can manually edit sequence order if needed

### Treatment Plan Export
**As a dentist**, I want to export treatment plans as PDF so that I can share them with patients and colleagues.

**Acceptance Criteria:**
- Can click "Export PDF" button from treatment plan page
- PDF includes:
  - Patient information
  - Treatment checklist
  - AI-generated remarks (all 6 sections)
  - Treatment sequences
  - Dental chart/notes
- PDF is professionally formatted
- Can save or share PDF from device

## Functional Requirements

### FR-1: Clinical Data Form
- Dedicated activity/fragment for clinical data entry
- Form validation for required fields
- Save to backend API endpoint
- Load existing clinical data
- Associate with patient and dental measurement

### FR-2: Treatment Case Detail Page Enhancement
- Add "Clinical Inputs" button to open clinical data form
- Display clinical data status (saved/not saved)
- Show last updated timestamp
- Enable/disable AI buttons based on clinical data availability

### FR-3: AI Remarks Generation
- API integration with backend remarks generation endpoint
- Display loading state during generation
- Show 6 sections with distinct visual design
- Save generated remarks to backend
- Display saved remarks on page load

### FR-4: Treatment Checklist
- RecyclerView adapter for 10 treatment categories
- Checkbox for accomplished status
- EditText for notes
- Color coding by category type
- Integration with dental measurements

### FR-5: AI Sequence Generation
- Enhanced sequence generation with dependencies
- Display sequences in card layout
- Show phase types and status
- Sequence reordering capability
- Save sequences to backend

### FR-6: PDF Export
- Generate PDF from treatment plan data
- Include all sections (patient info, checklist, remarks, sequences)
- Share intent for PDF file
- Store in device downloads folder

## Non-Functional Requirements

### NFR-1: Performance
- Clinical data form loads within 2 seconds
- AI generation completes within 30 seconds
- Smooth scrolling with long remarks

### NFR-2: UI/UX
- Consistent design with other redesigned pages
- `bg_app.png` background
- White MaterialCardViews
- Modern buttons and inputs
- Proper loading indicators

### NFR-3: Data Sync
- All data syncs with Laravel backend
- Same API endpoints as Vue app
- Compatible data structures

### NFR-4: Offline Support (Future)
- Cache clinical data locally
- Queue AI generation requests when offline
- Sync when connection restored

## API Endpoints Needed

### Clinical Data
- `GET /patients/{id}/clinical-data` - Fetch clinical data
- `POST /patients/{id}/clinical-data` - Save clinical data
- `PUT /patients/{id}/clinical-data/{clinicalDataId}` - Update clinical data

### AI Generation
- `POST /treatment-plans/{patientId}/generate-remarks` - Generate AI remarks
- `POST /treatment-plans/{patientId}/generate-sequence` - Generate AI sequences

### Treatment Plan
- `GET /treatment-plans/{patientId}` - Get complete treatment plan
- `POST /treatment-plans/{patientId}/export-pdf` - Export as PDF

## Data Models

### ClinicalData
```kotlin
data class ClinicalData(
    val clinicalDataId: Int,
    val patientId: Int,
    val measureId: Int?,
    val chiefComplaint: String,
    val dentistDiagnosis: String,
    val clinicalFindings: String,
    val vitalSigns: String,
    val medicalHistory: String?,
    val currentMedications: String?,
    val allergies: String?,
    val additionalNotes: String?,
    val generatedRemarks: TreatmentRemarks?,
    val generatedSequence: List<SequenceItem>?,
    val createdAt: String,
    val updatedAt: String
)
```

### TreatmentRemarks
```kotlin
data class TreatmentRemarks(
    val urgent: String,
    val systemic: String,
    val preparatory: String,
    val decisionPoint: String,
    val corrective: String,
    val maintenance: String
)
```

### TreatmentChecklistItem
```kotlin
data class TreatmentChecklistItem(
    val code: String, // A-J
    val type: String, // Category name
    val sequence: String, // Tooth numbers or order
    val accomplished: Boolean,
    val notes: String
)
```

## Out of Scope (Future Enhancements)
- Dental chart visual editor (currently in web only)
- Real-time collaboration on treatment plans
- Treatment plan versioning
- Custom treatment categories beyond the 10 standard ones
- Voice input for clinical data

## Success Metrics
- 100% feature parity with Vue/Laravel implementation
- All AI generation features functional
- Clinical data successfully syncs across platforms
- PDF export generates valid, readable documents

## Dependencies
- Laravel backend with treatment plan endpoints
- Gemini AI API for remarks/sequence generation
- PDF generation library (e.g., iTextPDF or PDFDocument)
- Existing Android app architecture

## Timeline Estimate
- Requirements: Complete
- Design: 2-3 hours
- Implementation: 15-20 hours
  - Clinical data form: 3-4 hours
  - AI remarks UI: 3-4 hours
  - Treatment checklist: 3-4 hours
  - Enhanced sequencing: 3-4 hours
  - PDF export: 3-4 hours
- Testing: 3-4 hours
- Total: 20-27 hours

## Risks & Mitigations
| Risk | Impact | Mitigation |
|------|--------|-----------|
| Backend API endpoints missing | High | Verify endpoints exist before starting |
| AI generation takes too long | Medium | Add timeout and retry logic |
| PDF generation complex | Medium | Use established library, simplify initial version |
| Data model mismatches | High | Review Laravel models before implementation |
