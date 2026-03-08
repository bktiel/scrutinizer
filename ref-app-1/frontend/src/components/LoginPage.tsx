import React, {useEffect, useState} from 'react';
import {Alert, Box, Button, Collapse, IconButton, Stack, TextField} from "@mui/material";
import {getIsSessionValid, loginUser} from "../services/api.ts";
import {useNavigate} from "react-router-dom";
import {decodeUserDetailCookie} from "../util/util.ts";
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutline";
import CloseIcon from "@mui/icons-material/Close";

const LoginPage = () => {
    const [username, setUsername] = useState<string>("")
    const [password, setPassword] = useState<string>("")
    const [displaySuccess, setDisplaySuccess] = useState<boolean>(false);
    const [displayError, setDisplayError] = useState<boolean>(false);
    const navigate = useNavigate();

    useEffect(() => {
        getIsSessionValid().then(res=>{
            if(res)
                navigate("/");
        })
    }, []);

    useEffect(() => {
        if (displaySuccess) {
            (new Promise(resolve => setTimeout(resolve, 3000))).then(() => setDisplaySuccess(false));
        }
    }, [displaySuccess]);

    useEffect(() => {
        if (displayError) {
            (new Promise(resolve => setTimeout(resolve, 3000))).then(() => setDisplayError(false));
        }
    }, [displayError]);

    const handleFormSubmit = async () => {
        if (username.length > 0 && password.length > 0) {
            await loginUser(username, password);
            const roles = decodeUserDetailCookie(document.cookie);
            if (roles.includes("DISPATCHER")) {
                setDisplaySuccess(true);
                navigate("/dispatcher")
            } else if (roles.includes("RESPONDER")) {
                setDisplaySuccess(true);
                navigate("/responder")
            } else {
                setDisplayError(true)
            }
        }

    }
    return (
        <Box>
            <Collapse in={displaySuccess}>
                <Alert
                    aria-label={'successAlert'}
                    iconMapping={{success: <CheckCircleOutlineIcon fontSize="inherit"/>,}}
                    action={
                        <Stack direction={"row"}>
                            <IconButton onClick={() => setDisplaySuccess(false)} id={"closeAlert"} name={"closeAlert"}
                                        aria-label={"closeAlert"}>
                                <CloseIcon/>
                            </IconButton>
                        </Stack>
                    }>
                    Login successful!
                </Alert>
            </Collapse>
            <Collapse in={displayError}>
                <Alert
                    aria-label={'errorAlert'}
                    severity="error"
                    action={
                        <Stack direction={"row"}>
                            <IconButton onClick={() => setDisplayError(false)} id={"closeErrorAlert"}
                                        name={"closeErrorAlert"} aria-label={"closeErrorAlert"}>
                                <CloseIcon/>
                            </IconButton>
                        </Stack>
                    }>
                    Login unsuccessful.
                </Alert>
            </Collapse>
            <Box pt="3em">
                <form onSubmit={(event: React.FormEvent<HTMLFormElement>) => {
                    event.preventDefault()
                    handleFormSubmit()
                    event.currentTarget.reset();
                }}>
                    <Stack display="flex" spacing={2} alignItems="center">

                        <TextField id="userNameInput" label="Username" value={username}
                                   onChange={(e) => setUsername(e.target.value)}/>
                        <TextField id="passwordInput" label="Password"
                                   type="password"
                                   onChange={(e) => setPassword(e.target.value)}/>
                        <Button aria-label="submitForm" id="submitForm" variant="contained" type="submit"
                                value={password}
                        >Login</Button>
                    </Stack>
                </form>
            </Box>
        </Box>)
}

export default LoginPage;