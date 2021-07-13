# CHANGELOG

## Next Release
### Internal
- Bump Sentry Gradle plugin to v2.0.1
- Clean up `gradle.properties`
- Extract product flavor build config fields into `defaultConfig`
- Bump Material Design Components to v1.4.0
- Add ADRs for SQL performance profiling
- Bump sqlite-android version to 3.36.0
- Add `minWidth` for phone number text input layout in `screen_registration_phone`
- Add `Drug`s table
- Migrate `BloodPressureHistoryScreen` to a fragment
- Migrate `BloodSugarHistoryScreen` to a fragment
- Delete patients and it's complete medical records when the retention time has passed
- Push records to server during sync in batches
- Update default protocol drugs

### Features
- [In Progress: 06 Jul 2021] Add support for finding a patient online from ID scan

### Changes
- New supported language: Tamil (Sri Lanka) [`ta-LK`], Sinhala [`si-LK`]
- [In Progress: 7 Jul 2021] Overdue list improvements
  - Change overdue list UI
  - Load overdue patients without phone number
  - Add option to download & share overdue list
  - Updated contact patient bottom sheet UI
  
### Fixes
- Fix invalid qr code error when scanning a valid Indian NHID
- Fix `ContactPatientBottomSheet` not going back to call patient view on back click in call later mode
- Fix overdue list not changing when switching facility from overdue screen
  
## On Demo
### Internal
- Migrate `OverdueAppointment` to add patients without phone number
- Migrate `kotlinx.android.parcel` usage to `kotlinx.parcelize`
- Change `CrashReporter` to be a facade instead of a discrete interface
- Show download and share buttons in overdue list
- Show invalid json error when `NHID` number is empty or less than 14 digits   
- Explicitly pin OkHttp version to 4.X
- Migrate `RecentPatientsScreen` to a fragment
- Paginate recent patients list
- Fix overdue list item UI indentation
- Bump Kotlin to v1.5.20
- Increase tappable area of overdue call button
- Add `retainUntil` column to the `Patient` table
- Remove unnecessary method from `Analytics`
- Bump dependencies for [Flipper](https://fbflipper.com)
- Bump `com.github.egslava:edittext-mask` -> '1.0.7'
- Bump AGP to v4.2.2
- Replace `GITHUB_ACCESS_TOKEN` with `SERVICES_ACCESS_TOKEN`

### Changes
- Updated translations: `pa-IN`, `hi-IN`, `te-IN`, `kn-IN`, `mr-IN`, `te-IN`, `sid-ET`, `kn-IN`, `ta-IN`, `bn-BD`, `bn_IN`, `so-ET`, `ti-ET`, `am-ET`, `ta-LK`, `om-ET`
- Click on overdue patient to open patient summary
- Show progress when loading overdue patient contact information
- Tap outside or swipe to dismiss the bottom sheets
- Tap outside to dismiss the dialogs

## 2021-07-02-7847
### Internal
- Use single list adapter in `InstantSearchScreen`

### Changes
- Added a question about hypertension treatment when creating patient

### Fixes
- Fix random crashes when instant search screen is in background

## 2021-06-28-7839
### Internal
- Use `ExperimentalGetImage` for `BitmapUtils#getBitmap`
- Bump Annotation library to v1.2.0
- Add Annotation experimental library
- Bump Room to v2.3.0
- Bump Paging to v3.0.0
- Prefill search query in `InstantSearchScreen`
- Paginate search results in `InstantSearchScreen`
- Replace usage of custom `Optional` class with `java.util.Optional`
- Add analytics events in teleconsult log after success
- Paginate overdue appointments list
- Fix instant search error views overlapping each other
- Remove usage of deprecated constructor from `MobiusDelegate`

### Features
- Add training videos depending on language for `Amharic` and `Oromo`

### Changes
- Updated translations: `hi-IN`, `bn-BD`, `te-IN`, `bn-IN`, `am-ET`, `pa-IN`, `ti-ET`, `kn-IN`, `mr-IN`, `ta-IN`, `so-ET`, `om-ET`
- New supported language: Sidama [`sid-ET`]
- Updated home screen illustrations

### Fixes
- Fix confirm reset pin error text is not hidden by default

## 2021-06-16-7826
### Internal
- Implement QR code JSON parser and extracted direct Moshi usage in `ScanSimpleIdEffectHandler`

## 2021-06-14-7821
### Internal
- Bump Kotlin to v1.5.10
- Bump AGP to v4.2.1
- Use `.editorconfig` for project code style
- Prefill NHID as `alternateId` in `PatientEntryScreen`
- Update facility picker search edit text hint
- Move `PatientPrefillInfo` to `patient` package to resolve coupling
- Add a new style and a theme overlay for `TextInputDatePicker` textfields
- Remove `ShortCodeResultsScreen`
- Deprecate `PatientRepository#search`
- Remove RxBinding2
- Use new method of SQL performance profiling (transform generated Room DAOs)
- Redact logs with `RecentPatient`

### Features
- Add NHID support in `PatientEntryScreen`
- Use `PatientPrefillInfo` for prefilling patient entry information
- Show NHID in `PatientEntryScreen`
- Highlight patient identifiers in search

### Fixes
- Fix user cannot see sync button
- Fix progress view for manual OTP entry screen is not hidden when login fails

## 2021-05-31-7801
### Internal
- Improve `ScanSimpleIdScreen` navigation
- Change labels to indicate the ID search
- Bump Mobius to v1.5.3
- Material Theming Migration
- Show patients with national health id when searching with from scan screen
- Remove old patient search
- Show `LinkIdWithPatientSheet` for Indian NHID
- Auto request reviewer for opened and re-opened PRs
- Update National Health ID strings
- Remove unwanted fields in `PatientPrefillInfo`
- Don't auto request review if a reviewer is already assigned
- Show error text when user scans invalid QR code
- Redact sensitive information from logs
- Implement Instant Search using BP Passport Number

### Features
- Add NHID support in `ScanSimpleIdScreen`
- Update title text and label in `ScanSimpleIdScreen`

### Changes
- Update translations: `ti-ET`, `pa-IN`, `om-ET`, `bn-IN`, `te-IN`, `hi-IN`, `am-ET`, `ta-IN`, `mr-IN`, `bn-BD`, `so-ET`, `kn-IN`
- Home screen improvements
    - Update home screen illustrations
    - Change patient search button text style

## 2021-05-17-7776
### Internal
- Add automatic performance profiling (reported to Mixpanel) for the Room database queries
- Show Indian national health ID in `PatientSummaryScreen`
- Bump Firebase dependencies
    - Firebase Performance Plugin to 1.3.5
    - Firebase Performance Client to 19.1.1
    - Google Services Plugin to 4.3.5
- Enable near realtime Firebase performance monitoring ([MORE INFO](https://firebase.google.com/docs/perf-mon/troubleshooting?authuser=0&platform=android#faq-real-time-data))
- Refactor `BpPassportSheet` to `ScannedQrCodeSheet`
- Show Indian national health ID in `EditPatientScreen`
- Migrate Gradle build scripts to use Kotlin DSL
- Stop reporting `SyncEvent` to analytics
- Bump RootBeer to v0.0.9
- Bump AGP to v4.2.0
- Bump Kotlin to v1.5.0
- Add a common color resource for all search string query highlights
- Bump SQLite version to 3.35.5
- Update `removeUntil` predicate in `Router#replaceKeyOfSameType`
- Fix attaching removed fragment to `FragmentTransaction` in `Router`
- Show error dialog when assigning a NHID to a patient with an existing ID

### Fixes
- Fix highlight color for instant search using phone number  

## 2021-05-03-7756
### Features
- Add Instant Search using Patient Identifiers (Bangladesh National id, Ethiopian medical record no., etc)

### Internal
- Move QR code scanner preview view inside `ScanSimpleIdScreen`
- Remove dialog & sheet from navigation back stack when dismissing
- Return null for `Cursor.string` extension if the column index is less than or equal to -1
- Add `IndiaNationalHealthId` as an identifier in `IdentifierType`
- Add sql query for instant search by numeric criteria
- Bump Sentry to v4.3.0

### Changes
- Rename `Scan BP passport` button to `Scan QR code`
- Show remove overdue appointment reasons in a separate screen
- Show list of patients when multiple patients have same BP passport

### Fixes
- Fix dead patients showing in overdue list

## 2021-04-20-7712
### Internal
- Make entire prescribed drugs list scrollable
- Bump AGP to v4.1.3
- Bump Kotlin to v1.4.32
- Bump Dagger to v2.33
- Bump LeakCanary to v2.6
- Bump CameraX dependencies
- Bump Fragment to v1.3.2
- Bump PlayServices Auth to v19.0.0
- Bump Moshi to v1.11.0
- Bump JUnit to v4.13.2
- Change facility picker and instant search highlight background color to yellow
- Update Ethiopian date separator pattern
- Migrate `ContactPatientBottomSheet` to a `BaseBottomSheet`
- Add Material motion in `ContactPatientBottomSheet` for handling view visibility changes
- Migrate `ScheduleAppointmentSheet` to a `BaseBottomSheet`
- Migrate `PatientsTabScreen` to a fragment
- Migrate `ReportsScreen` to a fragment
- Migrate `OverdueScreen` to a fragment
- Add `DatePickerKeyFactory` for getting date picker key based on `Country`
- Move `TextInputDatePickerSheet` behind a feature flag
- Migrate app to use ViewBinding
- Replace `kotlin-android-extensions` with `kotlin-parcelize`

### Features
- Show `TextInputDatePickerSheet` for Ethiopian users
- Add Ethiopian calendar support

### Changes
- Add Somali language option to language selection
- Request camera permissions when opening QR code scanner from instant search screen if not provided

### Fixes
- Fix date stepper showing black color when it's disabled
- Fix prescribed drugs item corner radius not being updated when order is changed

## 2021-04-05-7697
### Internal
- Trim start and end white spaces for instant search query
- Add drug stock option in progress
- Show custom error screen for drug stock web view when there is no internet connection
- Remove date of birth `DateTimeFormatter` as constructor param from `PatientRepository`

### Features
- Open BP passport scanning from search screen

### Changes
- Update translations: `mr-IN`, `bn-BD`, `ti-ET`, `te-IN`, `pa-IN`, `hi-IN`, `om-ET`, `ta-IN`, `kn-IN`, `am-ET`, `bn-IN`
- Change "Preferred facility" to "Assigned facility"
- Show 'CHANGE' button in appointment reminder bottom sheet
- Highlight search field and show keyboard when 'Add to existing patient' option is selected
- [Ethiopia] Show dates in Ethiopian calendar

### Fixes
- Fix separator between Age & DOB fields not visible in patient entry & edit screens

## 2021-03-29-7688
### Internal
- Add feature flag for village type ahead search

### Fixes
- Fix village type ahead crash when creating/editing patient

## 2021-03-22-7681
### Internal
- Migrate `LinkIdWithPatientView` to `LinkIdWithPatientSheet`
- Convert `screen_patient_summary` to use `ConstraintLayout`
- Bump Play Core to v1.10.0
- Add option to manually trigger GH Actions
- Migrate `SetupActivity` to the new navigation framework
- Show link id sheet after patient summary profile is loaded and link id is present
- Use personal access token for rebase action
- Fix PR comment commands running condition
- Add recipe for integrating a new sync resource
- Bump ML Kit barcode dependency
- Bump CameraX dependencies
- BP passport scanner improvements
  - Added tap to focus in QR code scanner preview
  - Switch to `ZxingQrCodeAnalyzer` when ML Kit is unavailable 

### Changes
- Updated translations: `bn-BD`, `te-IN`, `hi-IN`, `ta-IN`, `mr-IN`, `bn-IN`, `ti-ET`, `am-ET`, `pa-IN`, `om-ET`
- Show next button in the registration screens
- Implement village type ahead in patient entry/edit screens
- Show overdue count in home screen tabs
- Filter results in village type ahead in patient entry/edit screens

## 2021-03-15-7672
### Fixes
- Fix prescribed drugs list going beyond screen bounds

## 2021-03-08-7663
### Internal
- Load the current facility as a direct call instead of a reactive one in `OverdueEffectHandler`
- Migrate `PatientSummaryScreen` to a fragment
- Bump Dagger to v2.32
- Bump Google truth to v1.1.2
- Bump WorkManager to v2.5.0
- Fix the Room query thread pool executor to run all available threads
- Load list of `colonyOrVillage` from the `PatientAddress` table
- Change `TeleconsultRecordScreen` to a fragment
- Change `EditMedicinesScreen` to a fragment
- Only load the alphabetical patient log once when the screen is opened
- Add `Router#popUntilInclusive`

### Changes
- Updated translations: `bn-IN`, `ti-ET`, `om-ET`, `bn-BD`, `te-IN`, `kn-IN`, `mr-IN`, `hi-IN`, `pa-IN`, `am-ET`
- Show progress when linking bp passport to patient
- Show progress when saving medical history for new patient
- Change prescriptions end icon to a chevron icon instead of dropdown

## 2021-03-03-7655
### Internal
- Improve performance of the instant search queries by ~50%

### Changes
- Stop loading patients from other facilities in the alphabetical patient log

### Fixes
- Fix cannot exit patient summary screen after scheduling an appointment

## 2021-02-26-7647
### Internal
- Change `AppLockScreen` to a fragment

## 2021-02-22-7641
### Internal
- Bump Material Design Components to v1.3.0
- Bump ConstraintLayout to v2.0.4
- Add `Widget.Simple.TextField.Layout.PatientEntry.AutoComplete` style
- Bump Kotlin to v1.4.30
- Remove [JCenter](https://bintray.com/) from Maven repository sources
- Set patient status to `migrated` whenever an overdue appointment is removed for one of the following reasons
    - "Transferred to another public health facility"
    - "Moved to private practitioner"
- Build only the `PRODUCTION` release APK as part of the CI checks

### Changes
- Updated translations: `mr-IN`, `ta-IN`, `bn-BD`, `ti-ET`, `am-ET`, `kn-IN`, `bn-IN`, `hi-IN`, `te-IN`, `pa-IN`, `te-IN`, `om-ET`, `ta-IN`
- Add next button to phone number registration screen
- Change prescribed drugs toolbar title to `Medicines`
- Fix downloaded prescription background color
- Change UI for adding protocol/custom drugs
- Show toolbar progress indicator in facility picker screen
- Update sorting order of prescribed drugs

### Fixes
- Fix App crash when returning to the register patient screen before completing registration when patient date of birth is entered
- Fix prescription entry sheet UI
- Fix name string in link id with patient view

## 2021-02-11-7629
### Internal
- Disable strict mode crash for VM policy
- Redesign `LinkIdWithPatientView`
- Move to Dagger assisted inject
- Make instant search by name case insensitive
- Change `InstantSearchScreen` to a fragment
- Change `ScanSimpleIdScreen` to a fragment
- Change `HomeScreen` to a fragment
- Remove `ScanBpPassportActivity`
- Migrate `TheActivity` to the new navigation framework
- Add `CardViewDetector` lint for warning when using androidx/appcompat `CardView`
- Add `TextViewTextSizeDetector` lint for warning when using `TextView#textSize`
- Bump AGP to v4.1.2
- Fix bottom sheet dialog theme
- Add `NoopViewRenderer`
- Migrate `BpPassportSheet` to `BaseBottomSheet`
- Inject user's country code in `SentryCrashReporter`
- Migrate `ShortCodeSearchResultScreen` to a fragment
- Change `AccessDeniedScreen` to a fragment
- Remove deprecated Mobius delegate usage in `BloodSugarHistoryScreen`
- Migrate `TeleconsultSharePrescriptionScreen` to a fragment
- Change `AlertFacilityChangeSheet` to `BaseBottomSheet`
- Change `FacilityChangeScreen` to `BaseBottomSheet`
- Change `ConfirmFacilityChangeScreen` to `BaseBottomSheet`
- Change `PatientSearchResultsScreen` to a fragment

### Changes
- Updated the app icon

### Features
- Highlight patient name and number in Instant search

## 2021-01-25-7605
### Internal
- Rename `newbranch` script to `newbranch.sh`
- Purge the QA environment before running the instrumented tests instead of after in CI
- Add support for reporting screen changes to Analytics for the new navigation framework
- Migrate `ProgressBar` usage to `CircularProgressIndicator`
- Scroll to top when instant search results are updated
- Report time taken when loading all patients and search results in Instant Search
- Change `RegistrationPhoneScreen` to a fragment
- Change `RegistrationPinScreen` to a fragment
- Change `LoginPinScreen` to a fragment
- Change `RegistrationFullNameScreen` to a fragment
- Change `RegistrationConfirmPinScreen` to a fragment
- Show keyboard in Instant Search only when there is no identifier
- Add different adapters for showing all patients and search results in Instant Search
- Add ADR for the new navigation framework
- Use Dagger Factory to bind instances instead of `BindX` interfaces
- Bump Room dependency
- Bump CameraX dependency
- Bump Dagger dependency
- Clean up the abstract screen types
- Migrate `AuthenticationActivity` to the new navigation framework
- Improve instant search ordering

### Features
- Add Instant search

### Changes
- Updated translations: `bn-IN`, `mr-IN`, `hi-IN`, `pa-IN`, `bn-BD`, `am-ET`, `ti-ET`

## 2021-01-11-7589
### Internal
- Add shell env comment to `pre-push` hook
- Add the Router for the new navigation framework 
- Track code style and lint rules for project in VCS
- Add Instant search query
- Add `ScreenFragmentCompat` to support using the older view-based screens in the v2 navigation framework
- Add convenience classes for creating screens for the new navigation framework

### Fixes
- Fix BPpassport prefill value to have display value

### Changes
- Change home illustration for India

## 2020-12-31-7576
### Fixes
- Fix short code search screen crash

## 2020-12-28-7571
### Internal
- Add PR comment commands for running instrumented tests and rebasing PR
- Add breadcrumbs for different stages in search operations
- Cleanup `ImageSrcDetector`
- Bump tooling JDK to 11.0
- Migrate `ShortCodeSearchResultScreen` to Mobius

### Changes
- Updated translations: `pa-IN`, `te-IN`, `ti-ET`, `bn-IN`, `mr-IN`, `am_ET`

### Fixes
- Fix a crash that could happen when closing the edit medicines screen

## 2020-12-17-7559
### Internal
- Convert the `ImageProxy` to `Bitmap` when decoding the QR code using ML kit

## 2020-12-14-7554
### Internal
- Bump appcompat -> 1.2.0
- Bump CI JDK version to 11
- Revert view binding migration for `RecentPatientsView`
- Add `MLKitQrCodeAnalyzer`

### Changes
- Disable the change language feature on devices running Lollipop (API level 21, 22)
- Updated translations: `kn-IN`, `ta-IN`, `pa-IN`, `om-ET`

### Fixes
- Fix `shortCodeText` auto focusing in `ScanSimpleIdScreen`
- Fix app going back to home screen after scanning a BP passport

## 2020-12-03-7536
### Fixes
- Fix BP passport scanning not working

## 2020-12-01-7532
### Internal
- Move BP passport scan results handling to `HomeScreen`

### Fixes
- Fixed BP Passport scanning does not work after the first scan

## 2020-11-30-7529
### Features
- Refill prescriptions in edit medicine screen
- Add support for recording RBS, FBS, and PPBS blood sugars in two units: mmol/L or mg/dL

### Internal
- Add `recipes.md`
- Added blood sugar unit preference
- Move scanning of QR codes to a discrete activity
- Bump AGP to 4.1.1
- Convert blood sugar values to `mg/dL` when saving/updating

### Changes
- Disabled running the app on rooted devices
- Change label of district address fields in  patient entry and edit screens to "Zone" for Ethiopia users
- Updated translations: `ti-ET`, `pa-IN`, `ta-IN`, `te-IN`, `mr-IN`, `bn-IN`, `hi-IN`, `ta-IN`, `bn-BD`, `kn-IN`, `am-ET`
- Render blood sugar in `mmol/L` in summary view and history.
- Refill prescriptions in edit medicine screen

### Fixes
- Fix issue where the local user state can become inconsistent during registration

## 2020-11-09-7507
### Features
- Add `TeleconsultStatusSheet`
- Show `Next` in `ScheduleAppointmentSheet`

### Internal
- Update `TeleconsultRecord` to sync frequently
- Add `TeleconsultStatus` to `TeleconsultRecord`
- Mark strings used in prescription image as un-translatable
- Use `ENGLISH` locale when formatting prescription date
- Rename `TeleconsultRecord` `requestCompleted` to `requesterCompletionStatus`
- Bump the resync token for the Facility sync
- Bump AGP to 4.1.0
- Add `PrescribedDrug#refill` method
- Remove unnecessary sealed class types for different Business ID metadata versions
- Clean up `PatientRepository`
    - Change reactive calls for registering a patient to synchronous ones
    - Accept the ongoing patient entry as a parameter to the register patient method
    - Remove deprecated `Optional` class usages
- Move registration and login flows to a separate activity
- Update `appupdatedialog_body` string
- Enable ViewBinding
- Migrate `SplashScreen` to use ViewBinding

### Changes
- Update translations: `kn-IN`, `ta-IN`, `bn-IN`, `ti`, `bn-BD`, `pa-IN`, `mr-IN`, `te-IN`, `hi-IN`

### Fixes
- Fix teleconsult button in patient summary screen is broken on some devices

## 2020-10-26-7490
### Features
- Add `SelectDiagnosisErrorDialog` when diagnosis is not selected in `NewMedicalHistoryScreen` and `MedicalHistorySummaryView` 

### Changes
- Made "SMS Reminders" to be the default consent label on the patient screen unless otherwise specified on a country level
- Show toast message after saving prescription image on device
- Fix `ProgressMaterialButton` AVD not animating when initial state is not `in-progress`

### Internal
- Add `syncGroup` property to the `Facility` resource
- Remove custom WorkManager initializer
- Bump Android Gradle Plugin version to 4.0.2
- Add method to `AppDatabase` to clear all patient data not in the current sync group
- Update download and share `MaterialButtons` in `TeleconsultSharePrescriptionScreen` to `ProgressMaterialButtons`
- Purge unused data after a full sync completes
- Register patients in sync integration tests
- Stop registering a blood pressure measurement to associate a patient in tests with a facility
- Report database optimization events to analytics
- Delay purging of data from a different sync group for a fixed duration (remotely configurable) after switching facilities
- Add support for server controller resync when switching facility sync groups

### Fixes
- Fix app freeze when pressing enter/done with empty pin in lock screen

## 2020-10-13-7476
### Features
- Add `TeleconsultPrescriptionScreen`
- Add `TeleconsultSharePrescriptionScreen` 

### Changes
- Updated translations: `bn-IN`, `ti-ET`, `bn-BD`, `ta-IN`, `pa-IN`, `kn-IN`, `mr-IN`, `om-ET`
- Switch the positions of the teleconsultation and Save buttons in the `PatientSummaryScreen`

### Internal
- Remove `PrescribedDrugs` from `TeleconsultRecord`
- Create Kotlin extension for saving Optional preferences
- Modify `PrescribedDrug` to include `teleconsultation_id`
- Change `BloodPressureEntryEffectHandler` create and update measurement flows to be mostly synchronous
- Remove v1 of the QR code scanning flow
- Add custom traces in Firebase Performance Monitoring for the following flows:
  - Record new blood pressure measurement
  - Update existing blood pressure measurement
- Change registration and current facility ID columns to regular columns without foreign keys in `User`
- Add teleconsult record sync
- Change `RemoteConfigService` to pull updates synchronously
- Stop injecting screens in `TheActivity` via the static component
- Add support for sending teleconsult request via SMS
- Show medicines required error in `TeleconsultPrescriptionScreen`
- Remove `TeleconsultationApi` & load medical officers from `MedicalOfficer` table
- Fix prescribed drug frequency type adapter
- Request storage permission when downloading or sharing prescription
- Fix small UI inconsistencies in share prescription screen
- Hide keyboard when exiting from `TeleconsultPrescriptionScreen`
- Add file provider for sharing prescription image below Android 10

### Fixes
- Fix deeplink screens & warning dialogs displaying again after activity restart.
- Fix home screen tabs swiping
- Fix issue where sync events where being triggered on an individual sync level rather than at a group level

## 2020-10-01-7452
### Fixes
- App lock screen does not show if the app is exited and opened again while it is on the lock screen

## 2020-09-28-7446
### Features
- Add `TeleconsultRecordScreen`
- Show `TeleconsultLogNotAllowed` dialog when Medical officer is not allowed to teleconsult

### Internal
- Add `Capabilities` to User
- Change `TeleconsultFacilityInfoApi` endpoint
- Add methods to purge soft-deleted records from the database
- Add method to purge cancelled and visited appointments from the database
- Bump target SDK version to 30
- Add weekly recurring task to run maintenance tasks on the database
- Integrated Firebase Performance Monitoring

### Changes
- Updated translations for: `om-ET`, `ta-IN`, `bn-BD`, `mr-IN`, `hi-IN`, `ti-ET`
- Add ripple on touch for overdue patient name
- Set drug duration max limit

## 2020-09-15-7432
### Changes
- Open patient summary screen when patient name is clicked in overdue
- Stop loading count of overdue items on the home screen

### Internal
- Use UiRenderer for setting drug duration in `DrugDurationSheet`
- Add teleconsultation facility sync (Disabled for now until API endpoint is live: ETA 04-09-2020)
- Added `MedicineFrequencyBottomSheet` to update medicine frequency
- Add support for the teleconsultation record deeplink
- Add `frequency` & `durationInDays` to `PrescribedDrug`
- Add `ImageSrcDetector` lint for warning when using `ImageView#src`
- Add `TeleconsultRecord` and `TeleconsultRecordPayload`
- Bumped internal SQLite version to 3.32.2
- Change saving of app lock timestamp to an in-memory value
- Pin the number of threads used for running queries in Room based on the SQLite connection pool size
- Disable state saving and restoration for the search results view
- Stop querying for redundant facility in `LoggedInUserHttpInterceptor`

### Fixes
- Fixed BloodPressure removal not working

## 2020-09-03-7417
### Fixes
- Add a default RxJava error handler to ignore some classes of errors safely

## 2020-09-02-7414
### Fixes
- Fixed issue where patient details would not load for patients without an assigned facility ([#1127](https://app.clubhouse.io/simpledotorg/story/1127/patient-details-screen-does-not-load-for-patients-who-don-t-have-an-assigned-facility))

## 2020-09-01-7409
### Changes
- Added `SignatureActivity` to accept and save user signature
- Increase error text sizes
- Update error message of address fields in `PatientEntryScreen`

### Internal
- Migrated `LoggedOutOfDeviceDialog` to Mobius
- Migrated `ConfirmRemoveBloodPressureDialog` to Mobius
- Migrate `UpdatePhoneNumberDialog` to Mobius
- Migrated `LinkIdWithPatientView` to Mobius
- Migrate `PatientSearchResultsScreen` to Mobius
- Migrated `TheActivity` to Mobius
- Migrated `AppLockScreen` to Mobius
- Migrated `PatientSearchView` to Mobius
- Added `TeleConsultSuccessScreen` to notify teleconsultation recorded successfully
- Add `teleconsultPhoneNumber` in `User`, `LoggedInUserPayload` & `OngoingLoginEntry`
- Change `HomeScreen` to save and restore the state correctly
- Improved the user experience of loading the patient summary screen
- Optimized fetching of current facility by removing the unnecessary `User` parameter
- Clean up sync tests
- Change syncs to happen on a fixed pool of threads

## 2020-08-24-7397
### Fixes
- Fixed native crash on Android 9 with animated vector drawables ([#364](https://app.clubhouse.io/simpledotorg/story/364/fix-vectordrawable-native-crash))
- Fixed crash on putting the app in the background during registration facility selection ([#1030](https://app.clubhouse.io/simpledotorg/story/1030/app-crashes-during-registration-when-it-is-backgrounded))

## 2020-08-18-7386
### Features
- Add progress UI for registering a patient
- Add progress UI when scheduling appointment in `ScheduleAppointmentSheet`
- Add progress UI when editing a patient in `EditPatientScreen`
- Add progress UI when saving Blood pressure measurements in `BloodPressureEntrySheet`
- Add progress UI when saving Blood Sugar in `BloodSugarEntrySheet`

### Changes
- Updated translations: `ti`
- Update error message of address fields in `EditPatientScreen`

### Internal
- Migrated `LoginPinScreen` to Mobius
- Extracted facility selection UI into a separate component
- Migrated `ForgotPinCreateNewPinScreen` to Mobius
- Remove `AddToPatientSearch` screens and merge them with `PatientSearch` screens
- Migrated `OverdueScreen` to Mobius
- Migrated `FacilitySelectionActivity` to Mobius
- Migrated `FacilityChangeActivity` to Mobius
- Migrated `DrugSummaryView` to Mobius
- Migrated `ForgotPinConfirmPinScreen` to Mobius
- Change `OverdueScreen` to load data via the paging library
- Migrated `PatientSearchScreen` to Mobius
- Migrated `HelpScreen` to Mobius
- Migrated `HomeScreen` to Mobius
- Migrated `ScanSimpleIdScreen` to Mobius
- Migrate `EnterOtpScreen` to Mobius
- Setup the UI via an effect in `PatientEntryScreen`
- Setup the UI via an effect in `EditPatientScreen`
- Separate input fields providers based on country
- Add separate labels for "Street address" and "Village/Colony/Ward" labels in `PatientEntryScreen` and `EditPatientScreen` for Chennai facility groups

### Fixes
- Fixed issue where recent and overdue patients on home screen would not update after changing facility ([#742](https://app.clubhouse.io/simpledotorg/story/742/home-screen-does-not-update-when-changing-facilities))
- Fixed issue where the app would crash on restoring the home screen state ([#791](https://app.clubhouse.io/simpledotorg/story/791/the-app-crashes-when-navigating-back-to-the-home-screen-from-any-other-screen))
- Fix patient summary not opening on item click in all recent patients screen

## 2020-08-03-7364
### Features
- Show assigned facility in the patient summary screen
- Use patient's assigned facility to schedule an appointment
- Show `Transferred to` label in Overdue screen

### Internal
- Use `app:srcCompat` for `ImageView`
- Migrated `ReportsScreen` to Mobius
- Migrated `AddPhoneNumberDialog` to Mobius
- Migrated `RegistrationFacilitySelectionScreen` to Mobius
- Migrated `RecentPatientsView` to Mobius
- Migrated `RegistrationLoadingScreen` to Mobius
- Change selection of facility throughout the codebase to use `ItemAdapter`
- Bump patient resync token
- Migrated `RecentPatientsScreen` to Mobius
- Remove tracking of ongoing registration entry from `UserSession`

### Changes
- Updated translations: `bn_IN`, `am_ET`, `te_IN`, `pa_IN`, `hi_IN`, `bn_BD`
- Added Registration Facility name and date in `PatientSummaryScreen`
- Exclude deleted facilities from facility selection sheets
- Load overdue patients if current facility is same as patient assigned facility

### Fixes
- Fix crash when removing phone number for a patient that already has one ([LINK](https://app.clubhouse.io/simpledotorg/story/366/app-crashing-when-phone-number-is-empty-while-editing-patient))

## 2020-07-23-7353
### Internal
- Migrated `RegistrationPinScreen` to Mobius
- Migrated `ConfirmRemovePrescriptionDialog` to Mobius
- Migrated `RegistrationConfirmPinScreen` to Mobius
- Updated CI checks to look for [Clubhouse](https://app.clubhouse.io/simpledotorg) tickets instead of Pivotal Tracker
- Migrated `RegistrationLocationPermissionScreen` to Mobius
- Added patient registration and assigned facility columns to the patient model
- Replaced [ThreeTenBp](https://www.threeten.org/threetenbp/) for time APIs with [`java.time`](https://docs.oracle.com/javase/8/docs/api/java/time/package-summary.html)
- Enable vector drawable support library flag in app gradle

### Changes
- Add support for Medical Record Number in Ethiopia
- Add Amharic translation
- Updated translations: `mr_IN`, `om_ET`, `hi_IN`, `pa_IN`, `bn_BD`, `te_IN`, `ti_ET`

### Fixes
- Fixed occasional crash when starting a teleconsultation session ([LINK](https://app.clubhouse.io/simpledotorg/story/414/starting-a-teleconsultation-session-crashes-in-some-scenarios))

## 2020-07-07-7330
### Changes
- New Onboarding UI
- Show new registration label for recent patients in home screen
- Redesigned patient card in search results
- Changed blood sugar unit from `mg/dl` to `mg/dL`
- Show blood sugar high/low level labels
- Redesigned patient card in recent patients screen
- New illustrations for location permission screen, overdue screen, reports screen
- Show overdue appointment count badge in overdue tab
- Changed normal call buttons in the app from green to blue
- Updated translations: `bn_BD`, `kn_IN`, `mr_IN`, `am_ET`
- Ethiopia phone number validation

### Internal
- Migrated `RecentPatientItemType` to use `ItemAdapter`
- Moved storage of cached webviews (Progress, Help) to an implementation backed by the local database instead of files
- Migrated `RegistrationFullNameScreen` to Mobius
- Migrated `RegistrationPinScreen` to Mobius
- Make non-protocol drugs immutable
- Refactored logic around fetching location in facility selection screens to a common class
- Decoupled feature toggles from the remote configuration setup

### Fixes
- Fixed retrying on a failed teleconsultation fetch when in airplane mode
- Fixed showing diagnosis text in teleconsult message when diagnosis are unanswered
- Fixed keyboard not opening in app lock screen
- Fixed prescription created from a protocol drug not-showing in `EditMedicinesScreen`
- Fix changing of appointment facility does not update the facility

## 2020-06-22-7314
### Feature
- User can soft delete patient
- Add diagnosis and blood sugar readings to the teleconsultation message
- User can now select from multiple doctors when requesting a teleconsultation

### Changes
- Rename the button label to 'Next' when assigning a new BP passport to an existing patient
- Add visual indication for low blood sugars
- Updated translations: `ti_ET`, `om_ET`, `am_ET`

### Internal
- Change syncs on login to trigger at the usage site instead of in the `LoginUserWithOtp` class
- Support receiving multiple teleconsultation phone numbers
- Introduce a `UUID` generator class to ease testing record creation
- Migrated `RegistrationPhoneScreen` to Mobius
- Add support for [Java 8 desugaring](https://developer.android.com/studio/write/java8-support)
- Implement [Java `Optional`](https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html) APIs on the project `Optional` type
- Implement `ProgressMaterialButton`

### Fixes
- Fix blood pressure "High" label displaying when blood pressure is low.

## 2020-06-08-7298
### Feature
- Handle cases where the patient ID is not set correctly in the deeplink
- Show address for overdue patient

### Internal
- Migrated `ScheduleAppointmentSheet` to Mobius
- Fix PR description CI when using markdown syntax
- Add `DeletedReason` for `Patient`
- Improved analytics around sync events
- Updated translations: `bn_BD`
- Tighten Code Climate checks
- Add support for Android Studio 4.0
- Add support for building Android App Bundles (AABs)
- Migrate `PatientsScreen` to Mobius
- Migrated `SyncIndicatorController` to Mobius
- Add extension for extracting intent data from `ActivityResult`

### Fixes
- Fix camera crash when QR scanner view is paused/closed
- Fix fetching teleconsultation error when `PatientSummaryScreen` is restored

## 2020-05-21-7276
### Feature
- Add support for teleconsultation & deep-linking to open patient summary.

### Internal
- Lazy inject `facility` and `user` in all `EffectHandler` classes

### Fixes
- Fix the click issue in custom prescription on Medicines screen. [LINK](https://www.pivotaltracker.com/story/show/172784091)

## 2020-05-18-7270
### Feature
- Add support for calling the patient directly from the summary screen
- Show all BP passports of a patient while editing them
- New design for calling patient from the overdue list

### Internal
- Replace flow with navigation component in `SetupActivity`
- Updated translations: `bn_BD`, `kn_IN`, `hi_IN`, `mr_IN`, `pa_IN`
- Add CI check for fixing ellipsis's in strings.xml
- Extracted `BloodPressureReading` model as an embedded model from `BloodPressureMeasurement`
- Migrated `MedicalHistorySummaryView` to Mobius
- Add teleconsultation fields to the facility model
- Migrated `DosagePickerSheet` to Mobius
- Migrated `CustomPrescriptionEntryController` to Mobius

## 2020-05-04-7255
### Internal
- Add CI check for verifying that the CHANGELOG is updated
- Paginate data in blood sugar history page
- Use database view for `PatientSearchResult`
- Migrate `PrescribedDrugsScreenController` to Mobius

### Fixes
- Fixed how empty search error is shown

## 2020-04-20-7238
### Feature
- Introduce a facility confirmation screen whenever the user tries to perform one of the following actions immediately after changing facilities:
  - Register a new patient
  - Edit an existing patient
  - Schedule an appointment for a patient
  - Record a blood pressure measurement
  - Record a blood sugar measurement
  - Change the prescription
- Introduce a confirmation screen during user sign-up after selecting a facility

### Fixes
- Fixed bug where the 'Help" button on the home screen would distort if the facility name was very long
- Fixed occasional crash when searching for a patient by phone number

### Internal
- Sync help and progress reports when the language is changed in Settings
- Change the default appointment schedule period to 28 days
- Remove Whatsapp reminders from the reminder notification consent form when registering a patient for Bangladesh users
- Hide the secure calling toll-free message if the secure calling feature is not available
- Phone numbers are no longer hidden in search results
- Migrated to the new secure user login API endpoints
- Introduced an alternate QR code scanning module, built on AndroidX. This is controlled by a feature flag (`use_new_qr_scanner`)
- Hide the "Transgender" gender option during patient registration for Ethiopia users
- Add support for the Ethiopia address model
- Change the date format for recording blood sugars and blood pressures to "DD/MM/YYYY"

## 2020-03-30-7201
### Feature
- Add support for recording HbA1c blood sugars
- Add a diagnosis label on the overdue appointments

## 2020-03-30-7201
### Feature
- Add support for recording HbA1c blood sugars
- Add a diagnosis label on the overdue appointments

### Internal
- Changed api versioning to be at the endpoint level rather than global
- Integrated tool to report flaky instrumentation tests
- Changed automatic appointments to be scheduled when a blood sugar has been recorded in addition to a BP
- Consider patients with only blood sugars recorded in the following screens:
  - All patients in facility
  - Search results when searching by name or phone number
- Trigger syncs automatically on the following conditions:
  - An appointment is scheduled from the patient summary screen
  - A patient is registered
- Bump blood sugar sync api version to v4
- Ask the user to select a diagnosis for patients who don't have both diagnosis questions answered in patient summary

### Fixes
- Fixed occasional crash where the application would resume from background

## 2020-03-09-7161
### Feature
- Added support for editing blood sugar
- Added a new language: Afaan Oromo

### Internal
- Bumped AGP to 3.6.1
- Switched to R8 for minifying the APK
- Removed spellfix sqlite extension
- Unified mobius view and activity delegate

### Fixes
- Fixed crash when trying to edit patient DOB

## 2020-02-12-7090
### Feature
- Introduce a new design for the patient details page
- Add support for recording the patient diagnosis, based on the facility diabetes management support
- Added sync support for the recorded blood sugars
- Display the complete address on the patient search results and patient details page
- Add support for deleting the Bangladesh National ID from the edit patient page
- Add support for adding a Bangladesh National ID from the edit patient page
- Order the patient's prescribed drugs alphabetically on the patient details page

### Internal
- Simplify the computation of high risk label in overdue list
- Use the diabetes management flag from the facility instead of the remote config flag to toggle the diabetes view on patient summary
- Updated to the new visual styling of the CV history, Blood pressure, and Blood sugar widgets on the patient summary screen
- Bump the resync token for CV history sync to 2
- Bump the resync token for facility sync to 2

### Fixes
- Fix issue where the street and zone address fields would not be synced to the servers
- Fix issue where the units('days', 'weeks') in the appointment reminder sheet would not be translated
- Fix issue where formatted numerals in the app (BP, Age, Overdue days, etc) would use numeric glyphs in some languages instead of Arabic numerals

## 2020-01-27-7040
### Feature
- Introduce Diabetes management for tracking patient blood sugar

### Fixes
- Fix blank Bangladesh national ID getting saved while editing a patient

## 2020-01-14-7007
### Feature
- Updated patient entry consent text to include registration of minors
- Added/updated translations for the following languages: Telugu, Punjabi, Marathi, Kannada, Hindi

### Fixes
- Patient shows up on the Recent patients list of a transferred facility as soon as they are transferred
- Blank national ID gets generated for patients, which stops them from syncing

### Internal
- Removed the remote homescreen illustrations feature
- Bumped resync token for appointment sync
- Fixed incorrect migration for appointments which set all appointment types to `manual`
- Moved to the dynamic address field model for patient editing

## 2020-01-01-6917
### Feature
- Transfer patients to a different facility for follow-up

## 2019-12-16-6732
### Fixes
- Fixed the broken UI on the phone call sheet (https://www.pivotaltracker.com/story/show/169860653)

### Internal
- Use the selected country for connecting to the server
- Moved to the dynamic address field model for patient entry

## 2019-12-03-6640
### Internal
- Remove old edit patient screen
- Add a feature toggle for the phone masking feature
- Disable phone masking feature by default
- Remove Heap analytics

### Feature
- Add a country selection screen during the registration

## 2019-11-18-6468
### Internal
- Move Blood Pressure Entry sheet to Mobius
- Add breadcrumbs for tracking sizes of view saved states

## 2019-10-29-6197
### Feature
- Add a loading screen when the app database is being migrated after an update

### Internal
- Remove old AllPatientsInFacilityView code
- Make changing of screens in Flow synchronous
- Introduce an alternate activity (SetupActivity) as the launcher activity
- Move onboarding screen to the SetupActivity

## 2019-10-21-6057
### Feature
- Add consent text when registering a patient

### Internal
- Home screen banner illustrations are fetched remotely instead of hardcoded
- Migrate patient edit screen to Mobius
- Migrate patient new entry screen to Mobius
- Migrate onboarding screen to Mobius

## 2019-10-11-5948
### Internal
- Add remote toggle for screen change animations
- Add instrumentation for Flow events

## 2019-10-07-5787
### Feature
- Limit editing of BP measurements to one hour after being recorded. This is remotely configurable.

### Fixes
- Stop opening BP entry sheet immediately after registering a new patient.

### Internal
- Migrate AllPatientsInFacilityView to Mobius

## 2019-09-23-5587
### Feature
- Show Simple video for training user until some patients are registered

### Internal
- Trigger a full sync when the application starts

## 2019-09-09-5430
### Feature
- Exclude deleted patient records while searching by name or phone number
- Exclude deleted patient records when loading the alphabetical patient log
- Exclude deleted patients when loading overdue appointments
- Exclude deleted patients when finding patient by scanning BP Passport
- Exclude deleted appointments when loading overdue appointments
- Update designs for patient summary screen
- Show loader while patient search results load
- Add hindi translations to the app
- Search by short code from Scan BP passport screen

### Internal
- Add Mixpanel analytics
- Add device locale, timezone, and timezone offset headers to all requests 

## 2019-08-26-5229
### Feature
- Search by phone number support from the screen to find patient after scanning a BP Passport
- Older devices will automatically get cleared when the user logs in on a new device
- An exact date to schedule a patient visit can be picked from the schedule appointment screen

### Fixes
- Fix searching by phone number shows duplicate results for patients with multiple BPs

## 2019-08-12-5048
### Feature
- Search by phone number support from the screen to find patient without scanning
- Add a loading screen for registration
- Handle changes in user sync approval status

### Internal
- Remove unused Communication model
- Remove deprecated `validate` function and `Result` enum from `UserInputDateValidator`

### Fixes
- Fix taking the user to the production app play store page when install app update is clicked from sandbox or demo build
- Fix crash on overdue list when scrolling quickly when the screen is still loading appointments

## 2019-07-29-4866
### Feature
- All patients that have visited the current facility will be shown in the patient search screen
- Make BP date entry optional

### Internal
- Added forward compatibility support to MedicalHistory.Answer enum
- Added forward compatibility support to Gender enum
- Added forward compatibility support to PatientPhoneNumberType enum

### Fixes
- Fix camera not getting released properly and causing memory leaks
- Fix patient details not showing up sometimes in the phone call screen

## 2019-07-15-4657
### Feature
- Nudge users to update the app if it is older than expected

### Internal
- Change `LoggedInUser#status` to be a forward-compatible enum

### Fixes
- Fix overdue list to show last recorded BP, instead of last updated BP

## 2019-07-01-4522
### Feature
- Show "Last Seen" time of patient in recent patients list instead of "Last BP"

### Internal
- Change PinEntryCardView to accept PIN digest to verify externally instead of reading from UserSession

## 2019-06-17-4397
- Show 10 recent patients on the home screen, and the rest on a separate screen
- While calling patients, nurses can choose between phone call or secure anonymous call

## 2019-05-13-4013
- Added the BP Passport feature to the app
  - Scan a BP Passport and lookup a patient
  - Scan a BP Passport and link it with an existing patient
  - Scan a BP Passport and register a new patient

## 2019-04-22-3795
- Recent Patients list will not get affected by blood pressures and medical history entries that have been deleted
- Add a "Help" screen which is accessible from the home screen
- Show patient search results in two sections: ones who have visited the current facility, and the ones who have not

## 2019-03-25-3403
- Suggest nearby facilities to nurse, if switching after logging in
- Updated patient summary screen design
- Show “recent patients” on the home screen
- Sync status changes to “Pending” when patient data is updated
- Hide the sync indicator if nurse hasn’t been approved

## 3269-18Mar
- While creating a new account, facilities located nearby are automatically suggested 
- Scheduling an appointment shows new date options 

## 2856-25Feb
- Updated illustration on the patients tab 
- Improved app performance during first sync of patient data 
- Improve design of medical history screen 

## 2605-04Feb
- Blood pressures can be entered for dates in the past 
- New prescription drugs screen design 
- New medicine dosage picker design 

## 2301-04Jan
- Age and date of birth fields removed from patient search screen 
- Improved patient search algorithm
- Added new cancellation reasons for appointments 
- Updated the rules for identifying high-risk patients

## 2204-26Dec
- Fix: medical history syncing broke for some users

## 2116-17Dec
- Automatically submit PIN when 4 digits have been entered
- Show Yes/No/Unanswered buttons on medical history questions 
- Show option to change clinics from the home screen 
- Ask for confirmation when exiting Edit Patient screen without saving the edited information

## 2052-10Dec
- Dismiss keyboard while scrolling list of clinics 
- Highlight search query term when filtering clinics 

## 1991-03Dec
- Added ability to request a new OTP via SMS, while logging in 
- Blood pressure values are now editable for 24 hours
- Demographic data of patients is now editable
- "Very High" and "Extremely High" BPs are now shows as "High" 
- PIN entry is now protected against brute force attacks
- High risk patients are labelled in the overdue list
- Fix: crash when app was closed before entering OTP, while logging in 

## 1705-10Nov
- Patient searches with more than 100 results caused a crash
- Months in a date were being ignored, when calculating fuzzy age bounds

## 1678-08Nov
- Patient entry UI was cleaned up; entering a colony is now mandatory.
- Visual updates to the patient summary and home screen.
- Patients can be marked as "Dead" on the overdue screen, after which they will stop showing up on the overdue screen.
- Added option to schedule appointment for one month.
- Change age search to be more permissive for older patients.
- Sync frequency has been increased to once every fifteen minutes.
- Fix: Occasionally certain records did not get synced to the server.

## 1462-16Oct
- Add a helpful message to the home screen
- Fix: calling a patient from the overdue list sometimes used an incorrect phone number 

## 1420-11Oct
- Order facilities alphabetically in lists

## 1377-09Oct
- Fix: crash on opening patient summary if there was a BP recorded more than 6 months ago

## 1356-07Oct
- Fix: crash if multiple histories are present for the same patient 
- Fix: crash if empty systolic or diastolic blood pressure value is submitted
