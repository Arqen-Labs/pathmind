# Pathmind

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-00AA00?style=for-the-badge&logo=minecraft)](https://minecraft.net)
[![Fabric](https://img.shields.io/badge/Fabric-0.17.2-CC6E3E?style=for-the-badge&logo=modrinth)](https://fabricmc.net)
[![Java](https://img.shields.io/badge/Java-21+-FF6B6B?style=for-the-badge&logo=openjdk)](https://openjdk.java.net)
[![License](https://img.shields.io/badge/License-CC%20BY--NC%204.0-blue?style=for-the-badge)](https://creativecommons.org/licenses/by-nc/4.0/)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen?style=for-the-badge)](https://github.com/yourusername/pathmind)

A Minecraft Fabric mod that introduces a visual node editor system for creating complex workflows and automation sequences through an intuitive graphical interface.

## Features

### Visual Node Editor
- **Intuitive Interface**: Drag-and-drop node system with real-time visual feedback
- **Multiple Node Types**: Start, processing, and end nodes for different workflow stages
- **Smart Connections**: Automatic connection validation and visual feedback
- **Real-time Preview**: See your workflow structure as you build it

### Advanced UI System
- **Responsive Design**: Clean, modern interface that adapts to different screen sizes
- **Infinite Workspace**: Pan and zoom through your node graphs with smooth camera controls
- **Visual Grid**: Moving background grid that follows camera movement for better spatial awareness
- **Contextual Sidebar**: Organized node palette with easy access to all available components

### Interactive Controls
- **Drag & Drop**: Seamlessly move nodes around the workspace
- **Smart Panning**: Right-click or middle-click to pan around large node graphs
- **Home Button**: Quick return to origin with a single click
- **Socket Highlighting**: Visual feedback when hovering over connection points

### Minecraft Integration
- **Native Keybinds**: Customizable keybinds for quick access to the visual editor
- **Fabric Compatibility**: Built on the modern Fabric modding framework
- **Performance Optimized**: Efficient rendering and memory management

## Quick Start

### Prerequisites
- **Minecraft**: 1.21.8
- **Fabric Loader**: 0.17.2 or higher
- **Fabric API**: 0.133.4+1.21.8 or higher
- **Baritone API**: 1.15.0 (Fabric version) - [Download here](https://github.com/cabaletta/baritone/releases/download/v1.15.0/baritone-api-fabric-1.15.0.jar)
- **Java**: 21 or higher

### Installation

1. **Install Fabric Loader**
   - Download and install Fabric Loader for Minecraft 1.21.8
   - [Download from FabricMC](https://fabricmc.net/use/installer/)

2. **Install Fabric API**
   - Download the latest Fabric API for 1.21.8
   - [Download from Modrinth](https://modrinth.com/mod/fabric-api)

3. **Install Baritone API**
   - Download the Baritone API Fabric version 1.15.0
   - [Download from GitHub Releases](https://github.com/cabaletta/baritone/releases/download/v1.15.0/baritone-api-fabric-1.15.0.jar)
   - Place it in your `mods` folder

4. **Install Pathmind**
   - Download the latest Pathmind mod jar
   - Place it in your `mods` folder

5. **Launch and Enjoy!**
   - Start Minecraft with Fabric Loader
   - Use your configured keybind to open the visual editor

## How to Use

### Opening the Editor
- Use your configured keybind (default: check your keybinds menu)
- The visual editor will open in fullscreen mode

### Creating Node Workflows
1. **Add Nodes**: Drag nodes from the sidebar onto the workspace
2. **Connect Nodes**: Click and drag from output sockets to input sockets
3. **Organize Layout**: Drag nodes around to create clean, readable workflows
4. **Navigate**: Use right-click drag to pan around large node graphs

### Node Types
- **Start Node**: Entry point for your workflow
- **Processing Node**: Performs operations and data transformation
- **End Node**: Final output or result of your workflow

### Advanced Features
- **Infinite Panning**: Right-click and drag to move around the workspace
- **Home Button**: Click the home button in the bottom-right to return to origin
- **Visual Feedback**: Nodes grey out when dragged over the sidebar for deletion
- **Smart Connections**: Automatic connection replacement prevents multiple inputs

## Development

### Building from Source

1. **Clone the Repository**
   ```bash
   git clone https://github.com/yourusername/pathmind.git
   cd pathmind
   ```

2. **Generate Sources**
   ```bash
   ./gradlew genSources
   ```

3. **Import to IDE**
   - Import as a Gradle project
   - Wait for dependencies to resolve

4. **Build the Mod**
   ```bash
   ./gradlew build
   ```
   Output will be in `build/libs/`

5. **Run in Development**
   ```bash
   ./gradlew runClient
   ```

### Project Structure

```
src/main/java/com/pathmind/
├── nodes/           # Node system implementation
│   ├── Node.java    # Core node class
│   ├── NodeType.java # Node type definitions
│   └── NodeConnection.java # Connection handling
├── ui/              # User interface components
│   └── NodeGraph.java # Main node graph rendering
├── screen/          # Screen implementations
│   └── PathmindVisualEditorScreen.java # Main editor screen
├── PathmindMod.java # Main mod class
└── PathmindKeybinds.java # Keybind configuration
```

### Key Components

- **NodeGraph**: Manages node rendering, connections, and interactions
- **PathmindVisualEditorScreen**: Main screen with UI layout and event handling
- **Node**: Individual node implementation with socket management
- **NodeConnection**: Handles connection logic and validation

## Customization

### Node Types
The mod supports three main node types, each with distinct visual styling:
- **Start Nodes**: Green theme with output sockets
- **Processing Nodes**: Yellow theme with input and output sockets  
- **End Nodes**: Red theme with input sockets

### Visual Themes
- **Dark Theme**: Professional dark interface optimized for long editing sessions
- **Color-coded Sockets**: Input sockets on the left, output sockets on the right
- **Responsive Layout**: Adapts to different screen sizes and resolutions

## Version Information

| Component | Version |
|-----------|---------|
| **Mod Version** | 1.0.0 |
| **Minecraft Version** | 1.21.8 |
| **Yarn Mappings** | 1.21.8+build.1 (with :v2) |
| **Fabric Loader** | 0.17.2 |
| **Fabric API** | 0.133.4+1.21.8 |
| **Baritone API** | 1.15.0 |

## Contributing

We welcome contributions! Here's how you can help:

1. **Fork the Repository**
2. **Create a Feature Branch**: `git checkout -b feature/amazing-feature`
3. **Commit Your Changes**: `git commit -m 'Add amazing feature'`
4. **Push to the Branch**: `git push origin feature/amazing-feature`
5. **Open a Pull Request**

### Development Guidelines
- Follow Java coding conventions
- Add comments for complex logic
- Test your changes thoroughly
- Update documentation as needed

## License

This project is licensed under the **Creative Commons Attribution-NonCommercial 4.0 International License** (CC BY-NC 4.0).

This means you are free to:
- **Share**: Copy and redistribute the material in any medium or format
- **Adapt**: Remix, transform, and build upon the material

Under the following terms:
- **Attribution**: You must give appropriate credit
- **NonCommercial**: You may not use the material for commercial purposes

## Bug Reports & Feature Requests

Found a bug or have an idea? We'd love to hear from you!

- **Bug Reports**: [Open an Issue](https://github.com/yourusername/pathmind/issues/new?template=bug_report.md)
- **Feature Requests**: [Open an Issue](https://github.com/yourusername/pathmind/issues/new?template=feature_request.md)
- **General Discussion**: [Join our Discord](https://discord.gg/your-discord-link)

## Acknowledgments

- **FabricMC Team** for the amazing modding framework
- **Minecraft Community** for inspiration and feedback
- **Blender Foundation** for inspiring the node-based interface design

## Support

Need help? Here are some resources:

- **Documentation**: Check this README and in-game tooltips
- **Issues**: [GitHub Issues](https://github.com/yourusername/pathmind/issues)
- **Discord**: [Join our Community](https://discord.gg/your-discord-link)

---

<div align="center">

**Made with love for the Minecraft community**

[![GitHub](https://img.shields.io/badge/GitHub-Repository-black?style=for-the-badge&logo=github)](https://github.com/yourusername/pathmind)
[![Modrinth](https://img.shields.io/badge/Modrinth-Download-00D5AA?style=for-the-badge&logo=modrinth)](https://modrinth.com/mod/pathmind)

</div>