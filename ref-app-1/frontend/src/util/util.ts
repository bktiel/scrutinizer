import {AuthorityType} from "../types/AuthorityType.ts";

export const sampleSuperCookie = btoa(JSON.stringify([
    {"authority": "RESPONDER"},
    {"authority": "DISPATCHER"},
]));

export const decodeUserDetailCookie = (cookieString: string): string[] => {
    // const userDetailCookie = cookieString.slice(cookieString.indexOf('userDetail=') + 'userDetail='.length)
    const userDetailCookie = getCookie(cookieString, 'userDetail')
    if (userDetailCookie) {
        return JSON.parse(atob(userDetailCookie.value)).map((auth: AuthorityType) => auth["authority"]);
    }
    return [];
}

export const getCookie = (cookieString: string, targetCookieName: string) => {
    const cookieArr = cookieString.split(' ');
    if (cookieArr.length > 0) {
        let targetCookie = cookieArr.find(cookie => cookie.startsWith(targetCookieName + '='))
        if (targetCookie) {
            targetCookie = targetCookie.replace(';', '');
            return {name: targetCookieName, value: targetCookie.slice(targetCookieName.length + 1)}
        }
        return null;
    }
}