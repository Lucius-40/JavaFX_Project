# Getting Started:
## Step 1: Configure VSCode for java
First setup java in VSCode (recommended): https://code.visualstudio.com/docs/java/java-tutorial

## Step 2: Download and Configure JavaFX
Next, download javafx from here: https://gluonhq.com/products/javafx/

- Choose any of the LTS (Long Term Support) version (17 or 21). 
- You will get a .zip file. You need to extract that in any stable location (where all your porgrams files are already). For my case, I have extracted them into this location: "C:\Program Files\javafx-sdk-21.0.7"
- Then you need to add environment variable to the "system variables". Go to environment variables and choose new in system variables.
 1. variable name: "JAVAFX_HOME"
 2. variable value: "C:\Program Files\javafx-sdk-21.0.7"
- Then add "path" to the "system variables":
 1. Click on path and add "%JAVAFX_HOME%\bin" (you should find bin in the extracted direction).

You have set up the JAVAFX for global use!!!

## Step 3: Configure a Project
- Create a new project in vscode: press ctrl+shift+p and then write "create java project"
- click on that and the click on your desired package (For my case:"No Build Tools")
- Then you should find these things by default:
 1. .vscode
 2. bin
 3. lib
 4. src (Here your main code goes)
- You should configure/add these two files for using JavaFX: launch.json and settings.json:

### launch.json
```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Launch JavaFX App",
            "request": "launch",
            "mainClass": "App",
            "vmArgs": "--module-path \"C:/Program Files/javafx-sdk-21.0.7/lib\" --add-modules javafx.controls,javafx. fxml" //here, you need the path where you extracted the javafx
        }
    ]
}
```
### settings.json
```json
{
    "java.project.sourcePaths": ["src"],
    "java.project.outputPath": "bin",
    "java.project.referencedLibraries": [
        "C:/Program Files/javafx-sdk-21.0.7/lib/**/*.jar" //here, you need the path where you extracted the javafx
    ],
    "java.compile.nullAnalysis.mode": "automatic"
}
```

## The ideal file structure:

Online_Shop/
├── README.md
├── .vscode/
│   ├── launch.json
│   └── settings.json
├── bin/                          # Compiled classes
├── lib/                          # External dependencies
├── src/
│   ├── App.java                  # Main application entry point
│   ├── frontend/                 # JavaFX UI components
│   │   ├── controllers/          # FXML controllers
│   │   ├── views/               # FXML files
│   │   └── components/          # Reusable UI components
│   ├── backend/                 # Business logic
│   │   ├── models/              # Data models (Product, User, Order)
│   │   ├── services/            # Business services
│   │   └── utils/               # Utility classes
│   └── data/                    # File-based database
│       ├── storage/             # File I/O operations
│       └── repositories/        # Data access layer
├── resources/                   # Static resources
│   ├── fxml/                    # FXML layout files
│   ├── css/                     # Stylesheets
│   └── images/                  # Images and icons
└── database/                    # Database files
    ├── users.txt
    ├── products.txt
    └── orders.txt