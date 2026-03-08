import {decodeUserDetailCookie, getCookie} from "../../util/util.ts";
import {afterEach, expect} from "vitest";

describe('getCookie should', () => {

    afterEach(() => {
        vi.clearAllMocks()
    })
    it('should return cookie beginning with specified string', () => {
        document.cookie = "testCookie1=1234"
        document.cookie = "testCookie2=1234"
        expect(getCookie(document.cookie, 'testCookie1')).toEqual({name: 'testCookie1', value: '1234'})
        expect(getCookie(document.cookie, 'testCookie2')).toEqual({name: 'testCookie2', value: '1234'})
    });
    it('should return null if no cookie is found', () => {
        document.cookie = ""
        document.cookie = `testCookie1=1; expires=1 Jan 1970 00:00:00 GMT;`
        document.cookie = `testCookie2=1; expires=1 Jan 1970 00:00:00 GMT;`

        expect(getCookie(document.cookie, 'testCookie1')).toBeNull()
        expect(getCookie(document.cookie, 'testCookie2')).toBeNull()
    });
});

describe('decodeUserDetailCookie should', () => {
    const sampleUserDetailCookieSingle = btoa(JSON.stringify([
        {"authority": "RESPONDER"}
    ]));
    const sampleUserDetailCookieMultiple = btoa(JSON.stringify([
        {"authority": "RESPONDER"},
        {"authority": "DISPATCHER"}
    ]));
    it('should return one role when there is one role', () => {

        document.cookie = ""
        // document.cookie="userDetail=W3siYXV0aG9yaXR5IjoiUkVTUE9OREVSIn1d"
        document.cookie = `userDetail=${sampleUserDetailCookieSingle}`
        expect(decodeUserDetailCookie(document.cookie)).toEqual(["RESPONDER"])
    });
    it('should return multiple roles when there are multiple roles', () => {
        document.cookie = ""
        document.cookie = `userDetail=${sampleUserDetailCookieMultiple}`
        // document.cookie = "userDetail=W3siYXV0aG9yaXR5IjoiUkVTUE9OREVSIn0seyJhdXRob3JpdHkiOiJESVNQQVRDSEVSIn1d"
        expect(decodeUserDetailCookie(document.cookie)).toEqual(["RESPONDER","DISPATCHER"])

    });
    it('should return empty array when there are no roles', () => {
        document.cookie = ""
        document.cookie = `userDetail=1; expires=1 Jan 1970 00:00:00 GMT;`

        expect(decodeUserDetailCookie(document.cookie)).toHaveLength(0)
    });
});