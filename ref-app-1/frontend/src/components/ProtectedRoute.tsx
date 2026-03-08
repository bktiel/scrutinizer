import React, {useEffect, useState} from 'react';
import {getIsSessionValid} from "../services/api.ts";
import {decodeUserDetailCookie} from "../util/util.ts";
import {useNavigate} from "react-router-dom";
import Box from "@mui/material/Box";
import {Typography} from "@mui/material";

export type ProtectedRouteProps = {
    children: React.ReactNode;
    requiredRoles: string[];
}
const ProtectedRoute: React.FunctionComponent<ProtectedRouteProps> = (props) => {
    const [isValid, setIsValid] = useState<boolean | undefined>(undefined);
    const [isAuth, setIsAuth] = useState<boolean | undefined>(undefined)
    const navigate = useNavigate();

    useEffect(() => {
        getIsSessionValid().then(res => {
            let auth = true;
            for (const role of props.requiredRoles) {
                if (-1 === decodeUserDetailCookie(document.cookie).indexOf(role)) {
                    auth = false;
                }
            }
            setIsAuth(auth);
            setIsValid(res);
        })
    }, [setIsAuth, props.requiredRoles]);

    if (false===isValid) {
        navigate("/login")
    }
    if (false===isAuth) {
        return <Box display="flex" justifyContent="center">
            <Box pt="2em">
                <Typography variant="h2">Access Violation</Typography>
                <Typography align="center">You may not access this page.</Typography>
            </Box>
        </Box>
    }
    return <>{props.children}</>
};

export default ProtectedRoute;