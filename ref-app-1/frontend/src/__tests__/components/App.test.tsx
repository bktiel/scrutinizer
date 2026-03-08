import {describe} from "vitest";
import {render, screen} from "@testing-library/react";
import '@testing-library/jest-dom';
import App from "../../App.tsx";
import {userEvent} from "@testing-library/user-event";
import * as api from "../../services/api.ts";
import {sampleResponses} from "../mocks/handlers.ts";
import {RequestAssignmentType} from "../../types/RequestAssignmentType.ts";
import {sampleSuperCookie} from "../../util/util.ts";
import {ResponderType} from "../../types/ResponderType.ts";

describe("App", () => {
    it("should navigate to the requester page when requester button is clicked", async () => {
        vi.spyOn(api, 'getAllNineLineRequests').mockResolvedValue(sampleResponses);
        vi.spyOn(api, 'getAssignedNineLineRequests').mockResolvedValue(sampleResponses);
        vi.spyOn(api, "getAssignments").mockResolvedValue([] as RequestAssignmentType []);
        vi.spyOn(api, "getResponders").mockResolvedValue([] as ResponderType []);
        vi.spyOn(api, "getIsSessionValid").mockResolvedValue(true);
        document.cookie = `userDetail=${sampleSuperCookie}`

        render(<App/>);

        const link = screen.getByRole('button', {name: /requester/i});
        expect(link).toHaveTextContent(/requester/i);

        const locationTextbox = screen.getByRole('textbox', {name: /location/i});
        expect(locationTextbox).toBeVisible();
    });

    it("should navigate to the responder page when the responder button is clicked", async () => {
        vi.spyOn(api, 'getAllNineLineRequests').mockResolvedValue(sampleResponses);
        vi.spyOn(api, 'getIsSessionValid').mockResolvedValue(true);
        vi.spyOn(api, "getAssignments").mockResolvedValue([] as RequestAssignmentType []);
        document.cookie = `userDetail=${sampleSuperCookie}`

        render(<App/>);

        const link = screen.getByRole('button', {name: /responder/i});
        expect(link).toHaveTextContent(/responder/i);

        await userEvent.click(link);
        expect(screen.getByText("MEDEVAC Assignment")).toBeVisible();
    });

    it("should navigate to the dispatcher page when the dispatcher button is clicked", async () => {
        vi.spyOn(api, 'getAllNineLineRequests').mockResolvedValue(sampleResponses);
        vi.spyOn(api, "getAssignments").mockResolvedValue([] as RequestAssignmentType []);
        document.cookie = `userDetail=${sampleSuperCookie}`

        render(<App/>);

        const link = screen.getByRole('button', {name: /dispatcher/i});
        expect(link).toHaveTextContent(/dispatcher/i);

        await userEvent.click(link);
        expect(screen.getByText(/outstanding medevac requests/i)).toBeVisible();
    });
});