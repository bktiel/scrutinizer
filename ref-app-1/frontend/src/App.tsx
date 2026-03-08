import RequesterPage from "./components/RequesterPage.tsx";
import ResponderPage from "./components/ResponderPage.tsx";
import DispatcherPage from "./components/DispatcherPage.tsx";
import {createBrowserRouter, Outlet, RouterProvider} from "react-router-dom";
import Navbar from "./components/Navbar.tsx";
import {theme} from "./theme/theme.ts";
import {ThemeProvider} from "@mui/material";
import ProtectedRoute from "./components/ProtectedRoute.tsx";
import LoginPage from "./components/LoginPage.tsx";
import LogoutPage from "./components/LogoutPage.tsx";

function App() {
    const mainRouter = createBrowserRouter([
        {
            path: "/",
            element: <NavbarWrapper/>,
            children: [
                {
                    path: "/",
                    element: <RequesterPage/>,
                },
                {
                    path: "/login",
                    element: <LoginPage/>
                },
                {
                    path: "/requester",
                    element: <RequesterPage/>,
                },
                {
                    path: "/responder",
                    element: <ProtectedRoute requiredRoles={["RESPONDER"]}><ResponderPage/></ProtectedRoute>,
                },
                {
                    path: "/dispatcher",
                    element: <ProtectedRoute requiredRoles={["DISPATCHER"]}><DispatcherPage/></ProtectedRoute>,
                },
                {
                    path:"/logout",
                    element: <LogoutPage/>
                }
            ]
        }])
    return (
        <>
            <ThemeProvider theme={theme}>
                <RouterProvider router={mainRouter}/>
            </ThemeProvider>
        </>
    )
}

function NavbarWrapper() {
    return (
        <div>
            <Navbar/>
            <Outlet/>
        </div>
    )
}

export default App
