import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import Button from '@mui/material/Button';
import {useNavigate} from "react-router-dom";
import {decodeUserDetailCookie, getCookie} from "../util/util.ts";
import MenuOutlinedIcon from '@mui/icons-material/MenuOutlined';
import { IconButton, Menu, MenuItem } from "@mui/material";
import React, {useState} from "react";


function Navbar() {
    const navigate = useNavigate()
    const handleNavigation = (path: string) => {
        if (isMobileMenuOpen)
            handleMobileMenuClose()
        navigate(`/${path}`)
    }
    const [mobileMoreAnchorEl, setMobileMoreAnchorEl] =
        useState<null | HTMLElement>(null);
    const isMobileMenuOpen = Boolean(mobileMoreAnchorEl);

    const allPages = {
        requesterPage: 'requester',
        responderPage: 'responder',
        dispatcherPage: 'dispatcher',
        loginPage: 'login',
        logoutPage: 'logout'
    };

    const calculateAllowedPages = () => {
        const userPages = [allPages.requesterPage]
        if (-1 !== document.cookie.indexOf("userDetail")) {
            const userCookies = decodeUserDetailCookie(document.cookie);
            if (userCookies.includes('RESPONDER'))
                userPages.push(allPages.responderPage)
            if (userCookies.includes('DISPATCHER'))
                userPages.push(allPages.dispatcherPage)
            // userPages.push(allPages.logoutPage)
        } else {
            // userPages.push(allPages.loginPage)
        }
        return userPages;
    }

    const handleMobileMenuClose = () => {
        setMobileMoreAnchorEl(null);
    };
    const handleMobileMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
        setMobileMoreAnchorEl(event.currentTarget);
    };

    const mobileMenuId = 'primary-search-account-menu-mobile';
    const renderMobileMenu = (
        <Menu
            anchorEl={mobileMoreAnchorEl}
            anchorOrigin={{
                vertical: 'top',
                horizontal: 'right',
            }}
            id={mobileMenuId}
            keepMounted
            transformOrigin={{
                vertical: 'top',
                horizontal: 'right',
            }}
            open={isMobileMenuOpen}
            onClose={handleMobileMenuClose}
        >
            {calculateAllowedPages().map((page) => (
                <MenuItem
                    key={page}
                    sx={{my: 2, mx: 6, display: 'block'}}
                    component={"a"}
                    onClick={handleNavigation.bind(null, page)}
                >
                    {page.toUpperCase()}
                </MenuItem>
            ))}

        </Menu>
    );

    return (
        <AppBar position="static" sx={{paddingX: "1em", minHeight:"4em"}} >
            <Toolbar disableGutters sx={{display: {xs: "grid", md: "flex"}, gridAutoFlow: "column"}}>
                <img src="/product.svg" alt="SWF Logo"/>
                <Box sx={{flexGrow: 1, display: {xs: "none", md: 'flex'}, justifyContent:"center"}}>
                    {calculateAllowedPages().map((page) => (
                        <Button
                            key={page}
                            sx={{my: 2, mx: 6, display: 'block'}}
                            component={"a"}
                            onClick={handleNavigation.bind(null, page)}
                        >
                            {page}
                        </Button>
                    ))}
                </Box>
                <Box display="flex" flexDirection="row" justifySelf={{xs: "flex-end"}}>
                    <Box sx={{display: {xs: 'flex', md: 'none'}}}>
                        <IconButton
                            size="large"
                            aria-label="show more"
                            aria-controls={mobileMenuId}
                            aria-haspopup="true"
                            onClick={handleMobileMenuOpen}
                            color="inherit"
                        >
                            <MenuOutlinedIcon/>
                        </IconButton>
                    </Box>
                        {(getCookie(document.cookie, "userDetail")) ?
                            <Button variant="contained" aria-label="Logout" onClick={handleNavigation.bind(null, allPages.logoutPage )}>Logout</Button>
                            : <Button variant="contained" aria-label="Login" onClick={handleNavigation.bind(null, allPages.loginPage )}>Login</Button>}

                </Box>
            </Toolbar>
            {renderMobileMenu}
        </AppBar>
    );
}

export default Navbar;