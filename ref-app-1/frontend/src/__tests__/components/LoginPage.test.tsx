import {render, screen, waitFor} from "@testing-library/react";
import LoginPage from "../../components/LoginPage.tsx";
import * as api from "../../services/api.ts";
import {userEvent} from "@testing-library/user-event";
import {afterEach, expect} from "vitest";
import {createMemoryRouter, MemoryRouter, RouterProvider} from "react-router-dom";

describe('when login page is rendered', () => {
    const testUsername = "batman"
    const testPassword = "robin"

    const setupRouter = () => {
        return createMemoryRouter([
            {
                path: "/",
            },
            {
                path: "/login",
                element: <LoginPage/>
            },
            {
                path: "/responder",
                element:<>Dummy Responder</>
            }
        ], {
            initialEntries: ['/login'],
            initialIndex: 0
        })
    }

    afterEach(()=>{
        vi.resetAllMocks()
    })

    it('should have an input for username and password and a submit button', () => {
        render(<MemoryRouter><LoginPage/></MemoryRouter>);
        const usernameInput = screen.getByLabelText('Username')
        const passwordInput = screen.getByLabelText('Password')
        const submitButton = screen.getByRole('button', {name: 'submitForm'})
        expect(usernameInput).toBeVisible()
        expect(passwordInput).toBeVisible()
        expect(submitButton).toBeVisible()
    });

    it('should launch a post request to the backend to verify login', async () => {
        const loginUserSpy = vi.spyOn(api, "loginUser").mockResolvedValue(true);
        userEvent.setup();
        render(<MemoryRouter><LoginPage/></MemoryRouter>);
        const usernameInput = screen.getByLabelText('Username')
        const passwordInput = screen.getByLabelText('Password')
        const submitButton = screen.getByRole('button', {name: 'submitForm'})
        await userEvent.type(usernameInput, testUsername);
        await userEvent.type(passwordInput, testPassword);
        await userEvent.click(submitButton);
        expect(loginUserSpy).toBeCalledTimes(1);
    });

    it('should not launch a post request if the inputs are not valid', async () => {
        const loginUserSpy = vi.spyOn(api, "loginUser").mockResolvedValue(true);
        userEvent.setup();
        render(<MemoryRouter><LoginPage/></MemoryRouter>);
        const usernameInput = screen.getByLabelText('Username')
        const submitButton = screen.getByRole('button', {name: 'submitForm'})

        await userEvent.type(usernameInput, testUsername);
        await userEvent.click(submitButton);
        expect(loginUserSpy).toBeCalledTimes(0);
    });

    it('should redirect to /responder when logged in as user with responder role', async () => {
        vi.spyOn(api, "loginUser").mockResolvedValue(true);
        const testRouter = setupRouter();
        const sampleUserDetailCookie = btoa(JSON.stringify([
            {"authority": "RESPONDER"}
        ]));

        userEvent.setup();
        render(<RouterProvider  router={testRouter} fallbackElement={<LoginPage/>}></RouterProvider>);
        const usernameInput = screen.getByLabelText('Username')
        const passwordInput = screen.getByLabelText('Password')
        const submitButton = screen.getByRole('button', {name: 'submitForm'})
        await userEvent.type(usernameInput, testUsername);
        await userEvent.type(passwordInput, testPassword);

        document.cookie = "JSESSIONID=FAKESESSIONID"
        document.cookie = `userDetail=${sampleUserDetailCookie}`
        await userEvent.click(submitButton);

        await waitFor(()=>{
            expect(testRouter.state.location.pathname).toEqual('/responder')
        })
    });
});