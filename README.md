# GoogleGlass WiFi Positioning System

## Overview

This project is a WiFi-based indoor positioning system designed for Google Glass. It uses WiFi signal strength measurements and particle filtering to estimate the user's location within a building.

## Features

- Real-time indoor positioning using WiFi signals
- Particle filter implementation for accurate location tracking
- Support for multiple floor plans and building layouts
- Visualization of positioning data
- Designed specifically for Google Glass wearable devices

## Getting Started

### Prerequisites

- Android SDK (API level 19 or higher recommended for Google Glass)
- Google Glass Development Kit (GDK)
- Java Development Kit (JDK 7 or higher)
- Android Studio or Eclipse with ADT plugin

### Installation

1. Clone this repository:
   ```bash
   git clone https://github.com/your-repo/GoogleGlass.git
   ```

2. Open the project in Android Studio or Eclipse:
   - For Android Studio: File > Open > Select the project directory
   - For Eclipse: File > Import > Android > Existing Android Code Into Workspace

3. Set up the Android SDK:
   - Ensure you have the Google Glass System Image installed
   - Create an Android Virtual Device (AVD) for Google Glass or connect a physical device

### Building the Project

1. In Android Studio:
   - Click "Build" > "Make Project"
   - The APK will be generated in the `bin/` directory

2. In Eclipse:
   - Right-click the project > "Export"
   - Select "Android" > "Export Android Application"
   - Follow the prompts to generate the APK

### Running the Application

1. Connect your Google Glass device via USB or use the AVD
2. Install the APK:
   ```bash
   adb install bin/Wifi\ PF.apk
   ```
3. Launch the application from the Google Glass menu

## Project Structure

- `src/` - Main Java source code
  - `cz.muni.fi` - Czech Technical University components
  - `edu.berkeley.wifi` - WiFi scanning and positioning logic
  - `pf.*` - Particle filter implementation and utilities
  - `thirdparty.fft` - Fast Fourier Transform utilities

- `res/` - Android resources
  - `layout/` - UI layouts
  - `drawable/` - Images and icons
  - `values/` - Strings, styles, and dimensions

- `assets/` - Additional assets including floor plans

- `bin/` - Compiled classes and APK files

## Configuration

The application uses WiFi signal strength data to estimate position. You may need to:

1. Provide floor plan data in the `assets/` directory
2. Configure WiFi access point coordinates in the code
3. Adjust particle filter parameters for your specific environment

## Usage

1. Launch the application on Google Glass
2. The system will automatically scan for WiFi signals
3. Your position will be estimated and displayed on the floor plan
4. Move around to see real-time position updates

## Troubleshooting

- Ensure WiFi is enabled on the device
- Verify that the floor plan data matches your physical environment
- Check that the required WiFi access points are available
- Adjust particle filter parameters if positioning is inaccurate

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Czech Technical University for particle filter components
- University of California, Berkeley for WiFi positioning research
- Google for the Glass Development Kit

## Contact

For questions or support, please contact the project maintainers.
