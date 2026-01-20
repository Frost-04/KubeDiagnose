# KubeDiagnose Frontend

A modern, dark-mode Kubernetes debugging dashboard built with React, TypeScript, and Tailwind CSS.

## Features

- 🔍 Debug Kubernetes Pods and Services visually
- 📊 See all Pods and Services in a namespace at a glance
- 🚨 Quick identification of Critical / Warning / Healthy resources
- 📋 Detailed diagnostic information with copyable commands
- 🔎 Manual search for specific pods or services
- 🌙 Dark mode only, clean UI

## Tech Stack

- React 18
- TypeScript
- Vite
- Tailwind CSS
- Axios
- React Router
- Lucide React (icons)

## Prerequisites

- Node.js 18+
- KubeDiagnose backend running on port 8080

## Getting Started

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

## Configuration

The app uses Vite's proxy to forward API requests to the backend during development.

For production, set the `VITE_API_BASE_URL` environment variable:

```bash
VITE_API_BASE_URL=http://your-backend-url npm run build
```

## Project Structure

```
src/
├── api/                    # API layer (Axios)
│   ├── client.ts           # Axios instance
│   ├── namespaces.ts       # Namespace API
│   ├── pods.ts             # Pod API
│   └── services.ts         # Service API
├── components/             # React components
│   ├── DetailsPanel/       # Resource details panel
│   ├── NamespaceDropdown/  # Namespace selector
│   ├── PodDetails/         # Pod diagnostic view
│   ├── PodTile/            # Pod list item
│   ├── SearchBar/          # Global search
│   ├── ServiceDetails/     # Service diagnostic view
│   ├── ServiceTile/        # Service list item
│   ├── StatusBadge/        # Status indicator
│   └── TopBar/             # Header with search
├── pages/                  # Page components
│   └── Dashboard.tsx       # Main dashboard
├── types/                  # TypeScript types
│   ├── namespace.ts        # Namespace types
│   ├── pod.ts              # Pod types
│   └── service.ts          # Service types
├── utils/                  # Utility functions
├── App.tsx                 # App router
├── main.tsx                # Entry point
└── index.css               # Global styles
```

## Usage

1. Start the KubeDiagnose backend
2. Run `npm run dev`
3. Open http://localhost:5173
4. Select a namespace from the dropdown
5. Click any Pod or Service to see diagnostic details
6. Use the search bar to debug specific resources
