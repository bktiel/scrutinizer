import {render, screen} from "@testing-library/react";
import '@testing-library/jest-dom'
import App from "../../App.tsx";
import {expect} from "vitest";
import {sampleSuperCookie} from "../../util/util.ts";

describe("Navbar", () => {

    it('should render the Navbar', async () => {
        document.cookie = `userDetail=${sampleSuperCookie}`
        render(<App/>)
        expect(await screen.findByRole('button', {name: /Requester/i})).toBeVisible()
        expect(await screen.findByRole('button', {name: /Responder/i})).toBeVisible()
        expect(await screen.findByRole('button', {name: /Dispatcher/i})).toBeVisible()
        expect(await screen.findByRole('button', {name: /Logout/i})).toBeVisible()
    })
    it('should render the SWF logo'), () => {
        document.cookie = `userDetail=${sampleSuperCookie}`
        render(<App/>)
        const logo = screen.getByRole('img');
        expect(logo).toHaveAttribute('src', '/product.svg');
        expect(logo).toHaveAttribute('alt', 'SWF Logo');
    }
})