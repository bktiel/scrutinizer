import {afterEach, describe, expect, it} from "vitest";
import RequesterPage from '../../components/RequesterPage.tsx';
import {render, screen, waitFor, within} from "@testing-library/react";
import '@testing-library/jest-dom';
import {userEvent} from "@testing-library/user-event";
import * as api from "../../services/api.ts";
import {nationalities, nbcs, NineLineRequestType} from "../../types/NineLineRequestType.ts";

const request: NineLineRequestType = {
    id: null,
    location: "56J MS 80443 25375",
    callsign: "922929/Arrow/6",
    patientnumber: 3,
    specialEquipment: ["Hoist", "Extraction Equipment", "Ventilator"],
    litterpatient: 1,
    precedence: 1,
    ambulatorypatient: 2,
    security: "P",
    markingMethod: ["Panel", "Pyro", "Smoke"],
    nationality: 1,
    nbc: 1,
    status: "Pending"
};

const submitRequest = async (req: NineLineRequestType) => {
    const precedences: string [] = ['Urgent', 'Not Urgent'];


    const submitButton = screen.getByRole("button");
    const locationInput = screen.getByLabelText("Location");
    const contactInput = screen.getByLabelText(/Radio Frequency\/ Call Sign\/ Suffix/i);
    const patientNumberInput = screen.getByLabelText(/Patient Number/i);
    const precedenceInput = screen.getByLabelText(/Precedence/i);
    const litterInput = screen.getByLabelText(/Litter Patient #/i);
    const ambulatoryInput = screen.getByLabelText(/Ambulatory Patient #/i);
    const securityInput = screen.getByLabelText(/Security at Pick-Up Site/i);
    const nationalityInput = screen.getByLabelText(/Patient Nationality and Status/i);
    const nbcInput = screen.getByLabelText(/NBC Contamination/i);

    await userEvent.type(locationInput, req.location);
    {
        req.callsign.length && await userEvent.type(contactInput, req.callsign);
    }

    await userEvent.click(patientNumberInput);
    await userEvent.click(await screen.findByRole('option', {name: `${req.patientnumber}`}));
    await userEvent.click(precedenceInput);
    await userEvent.click(await screen.findByRole('option', {name: precedences [req.precedence - 1]}));

    for (let i = 0; i < req.specialEquipment.length; i++) {
        await userEvent.click(screen.getByRole('checkbox', {name: req.specialEquipment [i]}));
    }

    await userEvent.click(litterInput);
    await userEvent.click(await screen.findByRole('option', {name: `${req.litterpatient}`}));

    await userEvent.click(ambulatoryInput);
    await userEvent.click(await screen.findByRole('option', {name: `${req.ambulatorypatient}`}));

    await userEvent.click(securityInput);
    await userEvent.click(await screen.findByRole('option', {name: `${req.security}`}));

    for (let i = 0; i < req.markingMethod.length; i++) {
        await userEvent.click(screen.getByRole('checkbox', {name: req.markingMethod [i]}));
    }

    await userEvent.click(nationalityInput);
    await userEvent.click(await screen.findByRole('option', {name: nationalities[req.nationality - 1]}));

    await userEvent.click(nbcInput);
    await userEvent.click(await screen.findByRole('option', {name: nbcs[req.nbc - 1]}));

    await userEvent.click(submitButton);
};

describe('Requester Page test - MGRS', () => {
    const requestSpy = vi.spyOn(api, 'submitNineLineRequest');

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('should provide a field for user to put MGRS location', async () => {
        render(<RequesterPage/>);

        // const field = screen.getByRole('textbox', {name: "mgrs"});
        const field = screen.getByLabelText('Location');
        expect(field).toBeVisible();
    });

    it('should display an alert on successful form submission with test "Request Submitted. A dispatcher will contact you soon."', async () => {
        requestSpy.mockResolvedValue({} as NineLineRequestType);
        const alertText = /Request Submitted. A dispatcher will contact you soon/i;

        render(<RequesterPage/>);

        await submitRequest(request);

        expect(requestSpy).toBeCalledTimes(1);

        const alert = await screen.findByRole("alert");

        expect(alert).toBeVisible();
        expect(within(alert).getByText(alertText)).toBeVisible();
        expect(within(alert).getByRole("button", {name: "viewSubmittedRequest"})).toBeVisible();
        expect(within(alert).getByRole("button", {name: "closeAlert"}));
    });
});

describe('Requester Page - Contact information', () => {
    const requestSpy = vi.spyOn(api, 'submitNineLineRequest');

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('should have a text field for radio call sign', () => {
        render(<RequesterPage/>);

        expect(screen.getByLabelText(/Radio Frequency\/ Call Sign\/ Suffix/i)).toBeVisible();
    });

    it('should accept submission with filled out callsign.', async () => {
        requestSpy.mockResolvedValue({} as NineLineRequestType);

        render(<RequesterPage/>);

        await submitRequest(request);

        expect(requestSpy).toBeCalledTimes(1);
        expect(screen.getByText(/Request Submitted. A dispatcher will contact you soon./i)).toBeVisible();
    });
});

describe('Requester Page - Patient Number', () => {
    vi.spyOn(api, 'submitNineLineRequest');

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('should have a text field for number of patient', () => {
        render(<RequesterPage/>);
        expect(screen.getByLabelText(/Patient Number/i));
    });
});

describe('Requester Page - Precedence', () => {
    const requestSpy = vi.spyOn(api, 'submitNineLineRequest');

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('should have a drop down selector for Precedence', () => {
        render(<RequesterPage/>);
        expect(screen.getByLabelText(/Precedence/i));
    });

    it('should receive correct precedence from form submit.', async () => {
        requestSpy.mockResolvedValue({} as NineLineRequestType);

        render(<RequesterPage/>);

        await submitRequest(request);

        expect(requestSpy).toBeCalledTimes(1);
    });
});

describe('Requester Page - Special Equipment', () => {
    const requestSpy = vi.spyOn(api, 'submitNineLineRequest');

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('should have a text field for special equipment', () => {
        render(<RequesterPage/>);
        expect(screen.getByText(/special equipment/i)).toBeVisible();
    });

    it('should give all equipment selected', async () => {
        requestSpy.mockResolvedValue({} as NineLineRequestType);

        render(<RequesterPage/>);

        await submitRequest(request);

        expect(requestSpy).toBeCalledTimes(1);
        expect(requestSpy).toHaveBeenCalledWith(request);
    });

    it('should select none when none of the checkbox is checked.', async () => {
        requestSpy.mockResolvedValue({} as NineLineRequestType);

        render(<RequesterPage/>);

        await submitRequest({...request, specialEquipment: []});

        expect(requestSpy).toBeCalledTimes(1);
        expect(requestSpy).toHaveBeenCalledWith({...request, specialEquipment: ["None"]});
    });

    it('should deselect "None" when other options are selected.', async () => {
        render(<RequesterPage/>);
        const checkboxes = screen.getByLabelText(/Special Equipment/i);
        expect(within(checkboxes).getByRole('checkbox', {name: /None/i})).toBeChecked();

        await userEvent.click(within(checkboxes).getByRole('checkbox', {name: 'Hoist'}));

        expect(await within(checkboxes).findByRole('checkbox', {name: /None/i})).not.toBeChecked();
    });
});

describe('Requester Page - Litter Patient', () => {
    vi.spyOn(api, 'submitNineLineRequest');

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('should have a dropdown for inputting litter patient number', () => {
        render(<RequesterPage/>);
        expect(screen.getByText(/Litter Patient #/i)).toBeVisible();
    });

    it('should have ten possible values (0-9) for litter patients', async () => {
        render(<RequesterPage/>);
        const litterPatientField = screen.getByRole('combobox', {name: /Litter Patient #/i});
        await userEvent.click(litterPatientField);

        expect(await screen.findAllByRole('option')).toHaveLength(10);
    });
});

describe('Requester Page - Ambulatory Patient', () => {
    const requestSpy = vi.spyOn(api, 'submitNineLineRequest');

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('should have a dropdown for inputting the ambulatory patient number', () => {
        render(<RequesterPage/>);
        expect(screen.getByText(/Ambulatory Patient #/i)).toBeVisible();
    });

    it('should send request with correct values of ambulatory patient number', async () => {
        requestSpy.mockResolvedValue({} as NineLineRequestType);

        render(<RequesterPage/>);

        await submitRequest(request);

        expect(requestSpy).toBeCalledTimes(1);
        expect(requestSpy).toBeCalledWith(request);
    });
});

describe('Requester Page - Security of Pickup Site', () => {
    const requestSpy = vi.spyOn(api, "submitNineLineRequest");

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('should have a drop down menu to select the security of pickup site with options.', async () => {
        render(<RequesterPage/>);

        const input = screen.getByLabelText(/Security at Pick-Up Site/i);

        expect(input).toBeVisible();

        await userEvent.click(input);

        expect(await screen.findByRole('option', {name: 'N'}));
    });

    it('should send the request with correct values of selected dropwdown option', async () => {
        requestSpy.mockResolvedValue({} as NineLineRequestType);
        render(<RequesterPage/>);
        await submitRequest(request);
        expect(requestSpy).toBeCalledTimes(1);
        expect(requestSpy).toBeCalledWith(request);
    });
});

describe('Requester Page - Marking Method of pick-up site', () => {
    const requestSpy = vi.spyOn(api, "submitNineLineRequest");

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('should display sequential checkbox options as well as the Method of Marking title on the page', async () => {
        render(<RequesterPage/>);
        const input = screen.getByText(/Method of Marking Pick-up Site/i);
        expect(input).toBeVisible();

        const checkboxes = screen.getByLabelText(/Marking Methods/i);
        expect(within(checkboxes).getAllByRole('checkbox')).toHaveLength(5);
    });

    it('should send all marking methods selected', async () => {
        requestSpy.mockResolvedValue({} as NineLineRequestType);

        render(<RequesterPage/>);

        await submitRequest(request);

        expect(requestSpy).toBeCalledTimes(1);
        expect(requestSpy).toBeCalledWith(request);
    });

    it('should send the none checkbox option when no checkboxes are selected', async () => {
        requestSpy.mockResolvedValue({} as NineLineRequestType);
        render(<RequesterPage/>);
        await submitRequest({...request, markingMethod: []});
        expect(requestSpy).toHaveBeenCalledWith({...request, markingMethod: ["None"]});
    });

    it('should deselect None when other options are selected.', async () => {
        render(<RequesterPage/>);

        const checkboxes = screen.getByLabelText(/Marking Methods/i);

        expect(within(checkboxes).getByRole('checkbox', {name: 'None'})).toBeChecked();

        await userEvent.click(within(checkboxes).getByRole('checkbox', {name: 'Other'}));

        expect(await within(checkboxes).findByRole('checkbox', {name: 'None'})).not.toBeChecked();
    });
});

describe('Requester Page - Nationality of patient and status', () => {
    const requestSpy = vi.spyOn(api, "submitNineLineRequest");
    afterEach(() => {
        vi.clearAllMocks();
    });

    it('should have a dropdown menu to select the nationality of the patient along with their status', async () => {
        render(<RequesterPage/>);
        const drop = screen.getByLabelText(/patient nationality and status/i);
        await userEvent.click(drop);
        expect(await screen.findByRole('option', {name: "EPW"}));
    });

    it('should send nationality information when submitted.', async () => {
        requestSpy.mockResolvedValue({} as NineLineRequestType);

        render(<RequesterPage/>);

        await submitRequest(request);

        expect(requestSpy).toBeCalledTimes(1);
        expect(requestSpy).toBeCalledWith(request);
    });
});

describe('Requester Page - NBC contamination', () => {
    const requestSpy = vi.spyOn(api, "submitNineLineRequest");

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('should have NBC Contamination dropdown menu', async () => {
        render(<RequesterPage/>);
        expect(screen.getByLabelText(/nbc contamination/i)).toBeVisible();
    });

    it('should send nbc contamination information when submitted', async () => {
        requestSpy.mockResolvedValue({} as NineLineRequestType);
        render(<RequesterPage/>);
        await submitRequest(request);
        expect(requestSpy).toBeCalledTimes(1);
        expect(requestSpy).toBeCalledWith(request);
    });
});

describe('Form Errors', () => {
    const requestSpy = vi.spyOn(api, "submitNineLineRequest");

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('should close alert when close button is clicked on success banner', async () => {
        requestSpy.mockResolvedValue({} as NineLineRequestType);

        render(<RequesterPage/>);

        await submitRequest(request);

        const banner = screen.getByRole('alert', {name: 'successAlert'});

        await userEvent.click(screen.getByRole('button', {name: "closeAlert"}));
        await waitFor(() => expect(banner).not.toBeVisible());
    });

    it('should normally display "MGRS Format" and then display error when invalid input is detected.', async () => {
        requestSpy.mockResolvedValue({} as NineLineRequestType);

        render(<RequesterPage/>);

        expect(screen.getByText(/MGRS Format/i)).toBeVisible();

        await submitRequest({...request, location: "asdf"});

        expect(requestSpy).toBeCalledTimes(0);
        expect(screen.getByText(/Please enter in correct MGRS format./i)).toBeVisible();
    });

    it('should reject submission that has empty callsign.', async () => {
        requestSpy.mockResolvedValue({} as NineLineRequestType);

        render(<RequesterPage/>);

        await submitRequest({...request, callsign: ""});

        expect(requestSpy).toBeCalledTimes(0);
        expect(screen.getByText(/Required field./i)).toBeVisible();
    });

    it('should display server error', async () => {
        requestSpy.mockRejectedValue(new Error());

        render(<RequesterPage/>);

        await submitRequest(request);

        expect(await screen.findByRole('alert', {name: 'errorAlert'})).toBeVisible();
        expect(await screen.findByText(/server error/i)).toBeVisible();
    });
});
