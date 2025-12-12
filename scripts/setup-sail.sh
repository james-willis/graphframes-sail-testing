#!/usr/bin/env bash

set -e

echo "🔍 Checking Sail installation..."

# Check if python/pip is installed
if ! command -v pip &> /dev/null && ! command -v pip3 &> /dev/null; then
    echo "❌ Error: pip not found. Please install Python 3 first."
    exit 1
fi

# Use pip3 if available, otherwise pip
PIP_CMD="pip"
if command -v pip3 &> /dev/null; then
    PIP_CMD="pip3"
fi

# Function to install with appropriate flags
install_sail() {
    echo "📦 Installing pysail..."
    
    # Try normal install first
    if $PIP_CMD install pysail 2>/dev/null; then
        return 0
    fi
    
    # If that fails (externally-managed-environment), use --break-system-packages
    echo "⚠️  Detected externally-managed environment. Installing globally..."
    if $PIP_CMD install --break-system-packages pysail; then
        return 0
    fi
    
    echo "❌ Installation failed. You may need to use a virtual environment or install with sudo."
    return 1
}

# Function to upgrade with appropriate flags
upgrade_sail() {
    echo "📦 Upgrading Sail..."
    
    # Try normal upgrade first
    if $PIP_CMD install --upgrade pysail 2>/dev/null; then
        return 0
    fi
    
    # If that fails, use --break-system-packages
    if $PIP_CMD install --upgrade --break-system-packages pysail; then
        return 0
    fi
    
    echo "❌ Upgrade failed."
    return 1
}

# Check if sail is installed
if ! command -v sail &> /dev/null; then
    echo "📦 Sail not found."
    if install_sail; then
        echo "✅ Sail installed successfully!"
    fi
    exit 0
fi

# Sail is installed, check version
INSTALLED_VERSION=$(sail --version 2>/dev/null | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1)
echo "📌 Installed version: $INSTALLED_VERSION"

# Check for latest version from PyPI
echo "🔍 Checking for updates..."
LATEST_VERSION=$($PIP_CMD index versions pysail 2>/dev/null | grep -oE 'Available versions: [0-9]+\.[0-9]+\.[0-9]+' | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1)

# Fallback method if pip index doesn't work
if [ -z "$LATEST_VERSION" ]; then
    LATEST_VERSION=$(curl -s https://pypi.org/pypi/pysail/json | grep -oE '"version":"[0-9]+\.[0-9]+\.[0-9]+"' | head -1 | grep -oE '[0-9]+\.[0-9]+\.[0-9]+')
fi

if [ -z "$LATEST_VERSION" ]; then
    echo "⚠️  Could not fetch latest version from PyPI"
    echo "✅ Sail is installed (version: $INSTALLED_VERSION)"
    exit 0
fi

echo "🌐 Latest version: $LATEST_VERSION"

# Compare versions
if [ "$INSTALLED_VERSION" = "$LATEST_VERSION" ]; then
    echo "✅ Sail is up to date (version: $INSTALLED_VERSION)"
    exit 0
fi

# Newer version available
echo ""
echo "🆕 A newer version of Sail is available!"
echo "   Installed: $INSTALLED_VERSION"
echo "   Latest:    $LATEST_VERSION"
echo ""
read -p "Would you like to upgrade? (y/N): " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
    if upgrade_sail; then
        NEW_VERSION=$(sail --version 2>/dev/null | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1)
        echo "✅ Sail upgraded successfully to version $NEW_VERSION!"
    fi
else
    echo "⏭️  Skipping upgrade. Using version $INSTALLED_VERSION"
fi
