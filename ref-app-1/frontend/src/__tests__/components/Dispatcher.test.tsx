import {afterEach, describe, expect, it} from "vitest";
import {render, screen, waitFor, within} from "@testing-library/react";
import '@testing-library/jest-dom';
import {userEvent} from "@testing-library/user-event";
import DispatcherPage from "../../components/DispatcherPage.tsx";
import * as api from "../../services/api.ts";
import {NineLineRequestType} from "../../types/NineLineRequestType.ts";
import {RequestAssignmentType} from "../../types/RequestAssignmentType.ts";
import {ResponderType} from "../../types/ResponderType.ts";

const sampleRequests: NineLineRequestType[] = [
    {
        id: 1,
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
        status: "Complete",
    },
    {
        id: 2,
        location: "16S EG 12345 12345",
        callsign: "922929/Arrow/7",
        patientnumber: 2,
        specialEquipment: ["Hoist", "Ventilator"],
        litterpatient: 1,
        precedence: 2,
        ambulatorypatient: 1,
        security: "N",
        markingMethod: ["Panel", "Smoke"],
        nationality: 1,
        nbc: 2,
        status: "Pending",
    }
];

const sampleAssignment: RequestAssignmentType[] = [
    {
        requestId: 1,
        responder: 1,
    },
];

describe("Dispatcher Page - View Requests", () => {
    const getAllSpy = vi.spyOn(api, 'getAllNineLineRequests');
    const getAllAssignmentsSpy = vi.spyOn(api, 'getAssignments');
    const getAllRespondersSpy = vi.spyOn(api, 'getResponders')

    const initMock = () => {
        getAllSpy.mockResolvedValue(sampleRequests);
        getAllAssignmentsSpy.mockResolvedValue([] as RequestAssignmentType []);
        getAllAssignmentsSpy.mockResolvedValue([] as RequestAssignmentType []);
        getAllRespondersSpy.mockResolvedValue([] as ResponderType[]);
    };

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('should have "Outstanding MEDEVAC Requests" as title.', () => {
        initMock();

        render(<DispatcherPage/>);
        expect(screen.getByText(/outstanding medevac requests/i));
    });

    it('should render a table', () => {
        initMock();

        render(<DispatcherPage/>);
        const tableElement = screen.getByRole('table');
        expect(tableElement).toBeVisible();
    });

    it('should have a checkbox, "Call Sign", "Precedence", "Special Equipment", "Security", "Marking", and "Details" table headers.', () => {
        initMock();

        render(<DispatcherPage/>);

        const checkBox = screen.getByRole('columnheader', {name: 'checkboxAll'});

        expect(checkBox).toBeVisible();

        const columnHeaders = screen.getAllByRole('columnheader');

        expect(columnHeaders).toHaveLength(8);

        expect(within(columnHeaders[1]).getByText('Location')).toBeVisible();
        expect(within(columnHeaders[2]).getByText('Call Sign')).toBeVisible();
        expect(within(columnHeaders[3]).getByText('Precedence')).toBeVisible();
        expect(within(columnHeaders[4]).getByText('Special Equipment')).toBeVisible();
        expect(within(columnHeaders[5]).getByText('Security')).toBeVisible();
        expect(within(columnHeaders[6]).getByText('Marking')).toBeVisible();
        expect(within(columnHeaders[7]).getByText('Details')).toBeVisible();
    });

    it('should make an API call to request all MEDEVAC requests on page load', async () => {
        initMock();

        render(<DispatcherPage/>);

        const tableElement = screen.getByRole('table');

        await waitFor(() => expect(tableElement).toHaveAttribute('aria-rowcount', '2'));

        const allRows = screen.getAllByRole('row');

        expect(allRows).toHaveLength(3);
    });

    it('should display data in table row', async () => {
        initMock();

        render(<DispatcherPage/>);

        const tableElement = screen.getByRole('table');

        await waitFor(() => expect(tableElement).toHaveAttribute('aria-rowcount', '2'));

        const allRows = screen.getAllByRole('row');

        expect(within(allRows [1]).getByText(sampleRequests [0].location));
        expect(within(allRows [1]).getByText(sampleRequests [0].callsign));
        expect(within(allRows [1]).getByText(/urgent/i));
        expect(within(allRows [1]).getByText(/hoist, extraction equipment, ventilator/i));
        expect(within(allRows [1]).getByText(/possible enemy/i));
        expect(within(allRows [1]).getByText(/panel, pyro, smoke/i));
    });

    it('should show a header with the number of selected rows when a checkbox is selected and unselected', async () => {
        initMock();

        render(<DispatcherPage/>);
        userEvent.setup();

        const tableElement = screen.getByRole('table');

        await waitFor(() => expect(tableElement).toHaveAttribute('aria-rowcount', '2'));

        const allRows = screen.getAllByRole('row');
        const checkboxes: HTMLElement[] = [];

        await userEvent.click(within(allRows [0]).getByRole('checkbox'));

        screen.debug(undefined, Infinity)

        expect(await screen.findByText(/2 selected/i)).toBeVisible();

        for (const checkbox of checkboxes) {
            await userEvent.click(checkbox);
        }

        expect(await screen.findByLabelText('tableTitle')).toBeVisible();
    });
});

describe('Dispatcher Page - Submit Assignment', () => {
    const getAllSpy = vi.spyOn(api, 'getAllNineLineRequests');
    const postSpy = vi.spyOn(api, 'postRequestAssignment');
    const getAllAssignmentsSpy = vi.spyOn(api, 'getAssignments');
    const getAllRespondersSpy = vi.spyOn(api, 'getResponders')


    const initMock = () => {
        getAllSpy.mockResolvedValue(sampleRequests);
        postSpy.mockResolvedValue([] as RequestAssignmentType []);
        getAllAssignmentsSpy.mockResolvedValue([] as RequestAssignmentType []);
        getAllRespondersSpy.mockResolvedValue([
            {
                id:1,
                callsign:"Dad"
            },
            {
                id:2,
                callsign:"Paul"
            }
        ] as ResponderType[])
    };

    const renderDispatcherPageAndRenderDialog = async () => {
        render(<DispatcherPage/>);
        userEvent.setup();

        const tableElement = screen.getByRole('table');

        await waitFor(() => expect(tableElement).toHaveAttribute('aria-rowcount', '2'));

        const allRows = screen.getAllByRole('row');

        await userEvent.click(within(allRows [0]).getByRole('checkbox'));
        await userEvent.click(screen.getByRole('button', {name: "assignButton"}));
    };

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('should display dialog when assign button is clicked.', async () => {
        initMock();

        render(<DispatcherPage/>);

        userEvent.setup();

        const tableElement = screen.getByRole('table');

        await waitFor(() => expect(tableElement).toHaveAttribute('aria-rowcount', '2'));

        const allRows = screen.getAllByRole('row');

        await userEvent.click(within(allRows [0]).getByRole('checkbox'));
        await userEvent.click(screen.getByRole('button', {name: "assignButton"}));

        screen.logTestingPlaygroundURL();

        expect(await screen.findByRole('dialog'));
    });

    it('should display dialog with content of responder and dropdown with all responders.', async () => {
        initMock();

        await renderDispatcherPageAndRenderDialog();

        const dialog = await screen.findByRole('dialog');
        const dropDown = await within(dialog).findByRole('combobox', {name: /responder/i});

        expect(within(dialog).getByText(/Responder/i)).toBeVisible();
        expect(dropDown).toBeVisible();

        await userEvent.click(dropDown);

        expect(await screen.findAllByRole('option')).toHaveLength(2);
        expect(await screen.findByRole("option", {name: /Paul/i})).toBeVisible();
        expect(await screen.findByRole("option", {name: /Dad/i})).toBeVisible();
    });

    it('should conduct "postRequestAssignemnt" API to assign dispatcher to the request.', async () => {
        initMock();

        await renderDispatcherPageAndRenderDialog();

        const dialog = await screen.findByRole('dialog');
        const dropDown = await within(dialog).findByRole('combobox', {name: /responder/i});

        await userEvent.click(dropDown);
        const redResponder = await screen.findByRole("option", {name: /Paul/i});
        await userEvent.click(redResponder);

        const assignButton = screen.getByRole("button", {name: /assign/i});
        await userEvent.click(assignButton);
        expect(postSpy).toBeCalledTimes(1);
    });
});

describe('Dispatcher Page - View Request Details', () => {
    const getAllSpy = vi.spyOn(api, 'getAllNineLineRequests');
    const getAllAssignmentsSpy = vi.spyOn(api, 'getAssignments');
    const getAllRespondersSpy = vi.spyOn(api, 'getResponders')

    const initMock = () => {
        getAllSpy.mockResolvedValue(sampleRequests);
        getAllAssignmentsSpy.mockResolvedValue([] as RequestAssignmentType []);
        getAllRespondersSpy.mockResolvedValue([
            {
                id:1,
                callsign:"Dad"
            },
            {
                id:2,
                callsign:"Paul"
            }
        ] as ResponderType[])
    };

    const renderPage = () => {
        initMock();

        const user = userEvent.setup();
        const container = render(<DispatcherPage/>);

        return {user, container};
    };

    const renderPageAndClickView = async () => {
        const {user, container} = renderPage();
        const tableElement = screen.getByRole('table');

        await waitFor(() => expect(tableElement).toHaveAttribute('aria-rowcount', '2'));

        const allRows = screen.getAllByRole('row');
        const firstRow = allRows [1];
        const viewButton = within(firstRow).getByLabelText(/viewDialog/i);

        await user.click(viewButton);

        const dialog = await screen.findByRole('dialog');

        return {user, container, dialog};
    };

    interface Dictionary<T> {
        [Key: string]: T;
    }

    const precedenceDictionary: string[] = [
        "Missing Information",
        "Urgent",
        "Urgent Surgical",
        "Priority",
        "Routine",
        "Convenience"
    ];

    const securityDictionary: Dictionary<string> = {
        "N": "No enemy troops in area",
        "P": "Possible enemy troops in area",
        "E": "Enemy troops in area",
        "X": "Enemy troops in area"
    };

    const nationalities: string [] = [
        "Missing Information",
        "USM",
        "USC",
        "NUSM",
        "NUSC",
        "EPW",
    ];

    const nbcs: string [] = [
        "None",
        "Nuclear",
        "Biological",
        "Chemical",
    ];

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('should display a dialog of request detail when a request row is clicked.', async () => {
        const {dialog} = await renderPageAndClickView();
        expect(dialog).toBeVisible();
    });

    it.each([
        [/MEDEVAC Request/i],
        [/Location/i],
        [/Call Sign/i],
        [/Number of Patients/i],
        [/Precedence/i],
        [/Special Equipment/i],
        [/Litter Patient/i],
        [/Ambulatory Patient/i],
        [/Security at Pick-up site/i],
        [/Method of Marking/i],
        [/Patient Nationality and status/i],
        [/NBC Contamination/i],
    ])('should display the rows of data upon view button selection', async (expectedText) => {
        const {dialog} = await renderPageAndClickView();
        expect(within(dialog).getByText(expectedText)).toBeVisible();
    });

    it.each([
        sampleRequests [0].location,
        sampleRequests [0].callsign,
        sampleRequests [0].patientnumber,
        precedenceDictionary [sampleRequests [0].precedence],
        sampleRequests [0].specialEquipment.join(", "),
        sampleRequests [0].litterpatient,
        sampleRequests [0].ambulatorypatient,
        securityDictionary [sampleRequests [0].security],
        sampleRequests [0].markingMethod.join(", "),
        nationalities [sampleRequests [0].nationality],
        nbcs [sampleRequests [0].nbc],
    ])('should display the rows of actual data upon view button selection', async (expected: any) => {
        const {dialog} = await renderPageAndClickView();
        expect(within(dialog).getByText(`${expected}`)).toBeVisible();
    });

    it('should display the responder box that allows dispatchers to see a list of responders', async () => {
        const {user} = await renderPageAndClickView();
        const dropdown = screen.getByRole("combobox", {name: /responder/i});

        await user.click(dropdown);

        expect(await screen.findAllByRole("option")).toHaveLength(2);
        expect(await screen.findByRole("option", {name: /paul/i})).toBeVisible();
        expect(await screen.findByRole("option", {name: /dad/i})).toBeVisible();
    });
});

describe('Dispatcher Page - Interaction Logic', async () => {
    const getAllSpy = vi.spyOn(api, 'getAllNineLineRequests');
    const getAllAssignmentsSpy = vi.spyOn(api, 'getAssignments');
    const postSpy = vi.spyOn(api, 'postRequestAssignment');
    const getAllRespondersSpy = vi.spyOn(api, 'getResponders')

    const initMock = () => {
        getAllSpy.mockResolvedValue(sampleRequests);
        getAllAssignmentsSpy.mockResolvedValue(sampleAssignment);
        postSpy.mockResolvedValue([] as RequestAssignmentType []);
        getAllRespondersSpy.mockResolvedValue([
            {
                id:1,
                callsign:"Dad"
            },
            {
                id:2,
                callsign:"Paul"
            }
        ] as ResponderType[])
    };

    const renderPage = () => {
        initMock();

        const user = userEvent.setup();
        const container = render(<DispatcherPage/>);

        return {user, container};
    };

    const renderPageAndClickView = async () => {
        const {user, container} = renderPage();
        const tableElement = screen.getByRole('table');

        await waitFor(() => expect(tableElement).toHaveAttribute('aria-rowcount', '1'));

        const allRows = screen.getAllByRole('row');
        const firstRow = allRows [1];
        const viewButton = within(firstRow).getByLabelText(/viewDialog/i);

        await user.click(viewButton);

        const dialog = await screen.findByRole('dialog');

        return {user, container, dialog};
    };

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('should filter already assigned requests on initial page load', async () => {
        renderPage();

        const tableElement = screen.getByRole('table');

        await waitFor(() => expect(tableElement).toHaveAttribute('aria-rowcount', '1'));

        expect(getAllAssignmentsSpy).toBeCalledTimes(1);
        expect(getAllSpy).toBeCalledTimes(1);
    });

    it('should remove a single row upon assigning single request', async () => {
        const {user, dialog} = await renderPageAndClickView();

        await user.click(screen.getByRole("combobox", {name: /responder/i}));
        await user.click(await screen.findByRole("option", {name: /paul/i}));

        getAllAssignmentsSpy.mockResolvedValue([...sampleAssignment, {requestId: 2, responder: 1}]);

        await user.click(within(dialog).getByRole("button", {name: /viewAssignButton/i}));

        expect(dialog).not.toBeVisible();

        await waitFor(() => expect(getAllSpy).toHaveBeenCalledTimes(2));
        await waitFor(() => expect(getAllAssignmentsSpy).toHaveBeenCalledTimes(2));

        const table = await screen.findByRole('table');

        await waitFor(() => expect(table).toHaveAttribute('aria-rowcount', '0'));
    });
});
