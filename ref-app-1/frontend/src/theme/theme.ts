import {createTheme} from '@mui/material/styles';
import {green} from '@mui/material/colors';


export const theme = createTheme({
    typography: {
        // fontFamily: ["Inter", "system-ui", "Avenir", "Helvetica", "Arial", "sans-serif"].join(",")
        fontFamily: ["GIfont"].join(",")
    },
    palette: {
        primary: {
            main: "#FFCC01",
            contrastText: "#000"
        },
        secondary: {
            main: green[500],
        },
        text: {
            primary: "#000",
        },

    },
    components: {
        MuiAppBar: {
            styleOverrides: {
                root: {
                    backgroundColor: "#000",
                    color: "#FFD11A"
                },
            }
        },
        MuiInputBase: {
            styleOverrides: {
                root: {
                    backgroundColor:"#E8E8E8"
                }
            }
        },
        //@ts-ignore
        MuiDataGrid: {
            styleOverrides: {
                columnHeader: {
                    backgroundColor:"#FFCC01"
                }
            }
        }
    }
});