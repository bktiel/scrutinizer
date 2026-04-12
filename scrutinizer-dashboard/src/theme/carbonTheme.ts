import { createTheme, alpha } from '@mui/material'

const carbonTheme = createTheme({
  palette: {
    mode: 'dark',
    primary: { main: '#00B8D4', light: '#62EBFF', dark: '#0088A3' },     // cyan accent
    secondary: { main: '#7C4DFF', light: '#B47CFF', dark: '#3F1DCB' },   // purple accent
    success: { main: '#00E676', light: '#66FFA6', dark: '#00B248' },
    warning: { main: '#FFAB00', light: '#FFD740', dark: '#FF8F00' },
    error: { main: '#FF5252', light: '#FF867F', dark: '#C50E29' },
    background: {
      default: '#0A0E14',      // very dark navy-black
      paper: '#111820',        // slightly lighter
    },
    text: {
      primary: '#E6EDF3',
      secondary: '#8B949E',
    },
    divider: alpha('#8B949E', 0.15),
  },
  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    h4: { fontWeight: 700, letterSpacing: '-0.02em' },
    h5: { fontWeight: 600, letterSpacing: '-0.01em' },
    h6: { fontWeight: 600 },
    subtitle1: { fontWeight: 500 },
    subtitle2: { fontWeight: 500, color: '#8B949E' },
    body2: { color: '#8B949E' },
  },
  shape: { borderRadius: 8 },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          scrollbarColor: '#1C2333 #0A0E14',
          '&::-webkit-scrollbar': { width: 8 },
          '&::-webkit-scrollbar-track': { background: '#0A0E14' },
          '&::-webkit-scrollbar-thumb': {
            background: '#1C2333',
            borderRadius: 4
          },
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: 'none',
          backgroundColor: '#111820',
          border: '1px solid rgba(139, 148, 158, 0.1)',
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          backgroundImage: 'none',
          backgroundColor: alpha('#111820', 0.8),
          backdropFilter: 'blur(10px)',
          border: '1px solid rgba(139, 148, 158, 0.1)',
          transition: 'border-color 0.2s ease-in-out, box-shadow 0.2s ease-in-out',
          '&:hover': {
            borderColor: 'rgba(0, 184, 212, 0.3)',
            boxShadow: '0 0 20px rgba(0, 184, 212, 0.05)',
          },
        },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          backgroundImage: 'none',
          backgroundColor: alpha('#0A0E14', 0.9),
          backdropFilter: 'blur(10px)',
          borderBottom: '1px solid rgba(139, 148, 158, 0.1)',
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: { textTransform: 'none', fontWeight: 600 },
        contained: {
          boxShadow: 'none',
          '&:hover': { boxShadow: '0 0 20px rgba(0, 184, 212, 0.2)' },
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          fontWeight: 600,
          fontSize: '0.75rem',
        },
      },
    },
    MuiDataGrid: {
      styleOverrides: {
        root: {
          border: '1px solid rgba(139, 148, 158, 0.1)',
          '& .MuiDataGrid-cell': {
            borderColor: 'rgba(139, 148, 158, 0.08)',
          },
          '& .MuiDataGrid-columnHeaders': {
            backgroundColor: '#0D1117',
            borderColor: 'rgba(139, 148, 158, 0.1)',
          },
          '& .MuiDataGrid-row:hover': {
            backgroundColor: 'rgba(0, 184, 212, 0.04)',
          },
        },
      },
    },
    MuiTab: {
      styleOverrides: {
        root: { textTransform: 'none', fontWeight: 500 },
      },
    },
    MuiDialog: {
      styleOverrides: {
        paper: {
          backgroundColor: '#141B24',
          border: '1px solid rgba(139, 148, 158, 0.15)',
        },
      },
    },
    MuiTextField: {
      styleOverrides: {
        root: {
          '& .MuiOutlinedInput-root': {
            '& fieldset': { borderColor: 'rgba(139, 148, 158, 0.2)' },
            '&:hover fieldset': { borderColor: 'rgba(0, 184, 212, 0.4)' },
          },
        },
      },
    },
    MuiTooltip: {
      styleOverrides: {
        tooltip: {
          backgroundColor: '#1C2333',
          border: '1px solid rgba(139, 148, 158, 0.2)',
          fontSize: '0.8rem',
        },
      },
    },
    MuiSelect: {
      styleOverrides: {
        root: {
          '& .MuiOutlinedInput-notchedOutline': {
            borderColor: 'rgba(139, 148, 158, 0.2)',
          },
        },
      },
    },
  },
})

export default carbonTheme
