import {useEffect} from "react";
import {logoutUser} from "../services/api.ts";
import {useNavigate} from "react-router-dom";

const LogoutPage = () => {
    const navigate=useNavigate();
    useEffect(() => {
        logoutUser().then(()=>{
            document.cookie = "userDetail" +'=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT;';
            document.cookie = "JSESSIONID" +'=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT;';
            navigate('/requester');
        })
    }, [navigate]);

    return (
        <></>)
}

export default LogoutPage;