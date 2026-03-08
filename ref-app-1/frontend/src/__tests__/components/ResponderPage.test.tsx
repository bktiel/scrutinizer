import {render, screen, waitFor, within} from "@testing-library/react";
import ResponderPage from "../../components/ResponderPage.tsx";
import * as api from "../../services/api.ts";
import {NineLineRequestType} from "../../types/NineLineRequestType.ts";
import {userEvent} from "@testing-library/user-event";
import {afterEach, expect} from "vitest";
import {sampleResponse} from "../mocks/handlers.ts";


describe('When responder page is render', () => {
    const getAllSpy = vi.spyOn(api, 'getAssignedNineLineRequests')
    const postSpy = vi.spyOn(api, 'updateNineLineRequests')
    const sampleRequests: NineLineRequestType[] = [
        {
            id: 6,
            location: "20",
            callsign: "2222",
            patientnumber: 22,
            precedence: 33,
            status: "Pending",
            specialEquipment: [],
            litterpatient: 0,
            ambulatorypatient: 0,
            security: "",
            markingMethod: [],
            nationality: 0,
            nbc: 4,
        },
        {
            id: 8,
            location: "20",
            callsign: "2222",
            patientnumber: 22,
            precedence: 33,
            status: "Pending",
            specialEquipment: [],
            litterpatient: 0,
            ambulatorypatient: 0,
            security: "",
            markingMethod: [],
            nationality: 0,
            nbc: 6,
        }
    ]

    afterEach(() => {
        vi.clearAllMocks();
    });

    const renderGridAndSelectAllCheckBoxes = async () => {
        render(<ResponderPage/>)
        userEvent.setup();
        const tableElement = screen.getByRole('grid');
        const headerRow = screen.getByRole('row');

        await waitFor(() => {
            expect(tableElement).toHaveAttribute('aria-rowcount', '3')
        })

        const allRows = screen.getAllByRole('row').filter(row => row !== headerRow);
        const checkboxes: HTMLElement[] = [];

        for (const row of allRows) {
            checkboxes.push(within(row).getByRole('checkbox'));
        }
        for (const checkbox of checkboxes) {
            await userEvent.click(checkbox);
        }
        return checkboxes;
    }

    it('should render a table', () => {
        getAllSpy.mockResolvedValue(sampleRequests);
        render(<ResponderPage/>)
        const tableElement = screen.getByRole('grid');
        expect(tableElement).toBeVisible();
    });
    it('should make an API call to request all medevac requests on page load', () => {
        getAllSpy.mockResolvedValue(sampleRequests);
        render(<ResponderPage/>)
        expect(getAllSpy).toBeCalledTimes(1);
    })
    it('should render the items in the API call', async () => {
        getAllSpy.mockResolvedValue(sampleRequests);
        render(<ResponderPage/>)
        const tableElement = await screen.findByRole('grid');

        await waitFor(() => {
            expect(tableElement).toHaveAttribute('aria-rowcount', '3')
        })
        const allRows = screen.getAllByRole('row')
        expect(allRows).toHaveLength(3)
    })
    it('should show a header with the number of selected rows when a checkbox is selected', async () => {
        getAllSpy.mockResolvedValue(sampleRequests);
        const checkboxes = await renderGridAndSelectAllCheckBoxes();

        expect(screen.getByText(`${checkboxes.length} Selected`)).toBeVisible()

        for (const checkbox of checkboxes) {
            await userEvent.click(checkbox);
        }

        expect(screen.queryByText(`${checkboxes.length} Selected`)).toEqual(null)
    });
    it('should display completion and trash icons on header when rows are selected', async () => {
        getAllSpy.mockResolvedValue(sampleRequests);
        await renderGridAndSelectAllCheckBoxes();
        expect(screen.getByRole('button', {name: "setRequestsCompleted"})).toBeVisible();
        expect(screen.getByRole('button', {name: "setRequestsDeleted"})).toBeVisible();
    })
    it('should show the "# requests completed" alert when user clicks setRequestsCompleted button with records selected', async () => {
        getAllSpy.mockResolvedValue(sampleRequests);
        postSpy.mockResolvedValue(sampleRequests.map(sampleReq => ({...sampleReq, status: "Complete"})))
        const checkboxes = await renderGridAndSelectAllCheckBoxes();
        const completeButton = screen.getByRole('button', {name: 'setRequestsCompleted'})
        await userEvent.click(completeButton)
        const completedAlert = await screen.findByRole('alert');
        expect(within(completedAlert).getByText(new RegExp(`${checkboxes.length} Request Completed`, "i"))).toBeVisible()
    });
    it('should unselect the checkbox once the completed button is clicked and table actionbar disappears', async () => {
        getAllSpy.mockResolvedValue(sampleRequests);
        postSpy.mockResolvedValue(sampleRequests.map(sampleReq => ({...sampleReq, status: "Complete"})))

        const checkboxes = await renderGridAndSelectAllCheckBoxes();
        const completeButton = screen.getByRole('button', {name: 'setRequestsCompleted'})
        await userEvent.click(completeButton)
        expect(screen.queryByText(`${checkboxes.length} Selected`)).toEqual(null);
        for (const checkbox of checkboxes) {
            expect(checkbox).not.toBeChecked()
        }

    })
    it('should make api call when comleted button is clicked and updated statuses for selected requests  should show in the data grid', async () => {
        getAllSpy.mockResolvedValue(sampleRequests.map(sampleReq => ({...sampleReq, status: "Pending"})));
        postSpy.mockResolvedValue(sampleRequests.map(sampleReq => ({...sampleReq, status: "Complete"})))
        await renderGridAndSelectAllCheckBoxes();
        const completeButton = screen.getByRole('button', {name: 'setRequestsCompleted'})
        await userEvent.click(completeButton)
        expect(postSpy).toBeCalledTimes(1);
        const tableElement = screen.getByRole('grid');
        expect(await within(tableElement).findAllByText(/Complete/i)).toHaveLength(sampleRequests.length);
    });
    it('should render a modal when buttons in the status column in the datagrid are clicked', async () => {
        // setup
        getAllSpy.mockResolvedValue(sampleRequests.map(sampleReq => ({...sampleReq, status: "Pending"})));
        render(<ResponderPage/>);
        userEvent.setup();
        // wait for table to render with expected elements
        const tableElement = await screen.findByRole('grid');
        const headerRow = screen.getByRole('row');
        await waitFor(() => {
            expect(tableElement).toHaveAttribute('aria-rowcount', '3')
        })

        const allRows = screen.getAllByRole('row').filter(row => row !== headerRow);
        // grab all buttons in the table
        const completeButtons: HTMLElement[] = [];
        for (const row of allRows) {
            completeButtons.push(within(row).getByRole('button', {name: "getRequestDetails"}));
        }
        expect(completeButtons).toHaveLength(2);
        // user clicks on button, should show modal
        await userEvent.click(completeButtons[0]);
        const detailModal = screen.getByRole("dialog");
        expect(within(detailModal).getByText(/MEDEVAC Request/i)).toBeVisible();
    });

    it('should render the details of the MEDEVAC request based on the row that is clicked with the status button', async () => {
        const getByIdSpy = vi.spyOn(api, 'getNineLineRequestsById')
        getAllSpy.mockResolvedValue(sampleRequests.map(sampleReq => ({...sampleReq, status: "Pending"})));
        getByIdSpy.mockResolvedValue(sampleResponse);
        render(<ResponderPage/>);
        userEvent.setup();
        // wait for table to render with expected elements
        const tableElement = await screen.findByRole('grid');
        const headerRow = screen.getByRole('row');
        await waitFor(() => {
            expect(tableElement).toHaveAttribute('aria-rowcount', '3')
        })

        const allRows = screen.getAllByRole('row').filter(row => row !== headerRow);
        // grab all buttons in the table
        const completeButtons: HTMLElement[] = allRows.map((row, index) => {
            const thisButton = within(row).getByRole('button', {name: "getRequestDetails"});
            expect(thisButton).toHaveTextContent(sampleRequests[index].status);
            return thisButton;
        })
        await userEvent.click(completeButtons[0]);
    });
    it('should display all the details for each field of the MEDEVAC request upon clicking the status button', async () => {
        getAllSpy.mockResolvedValue(sampleRequests.map(sampleReq => ({...sampleReq, status: "Pending"})));
        render(<ResponderPage/>);
        userEvent.setup();
        const tableElement = await screen.findByRole('grid');
        const headerRow = screen.getByRole('row');
        await waitFor(() => {
            expect(tableElement).toHaveAttribute('aria-rowcount', '3')
        })
        const allRows = screen.getAllByRole('row').filter(row => row !== headerRow);
        // grab all buttons in the table
        const completeButtons: HTMLElement[] = allRows.map((row, index) => {
            const thisButton = within(row).getByRole('button', {name: "getRequestDetails"});
            expect(thisButton).toHaveTextContent(sampleRequests[index].status);
            return thisButton;
        })
        const thisButton = completeButtons[0];
        await userEvent.click(thisButton);
        const modal = screen.getByRole('dialog')
        const modal_rows = [
            "Status",
            "Location",
            "Call Sign",
            "Number of Patients",
            "Precedence",
            "Special Equipment",
            "Litter Patient",
            "Ambulatory Patient",
            "Security at Pick-up Site",
            "Method of Marking",
            "Patient Nationality and status",
            "NBC Contamination"
        ]
        for (const field of modal_rows) {
            const thisModalRow = within(modal).getByText(field).parentElement;
            if (thisModalRow) {
                switch (field) {
                    case "Status":
                        expect(within(thisModalRow).getAllByText(sampleRequests[0].status)[0]).toBeVisible()
                        break;
                    case "Location":
                        expect(within(thisModalRow).getAllByText(sampleRequests[0].location)[0]).toBeVisible()
                        break;
                    case "Call Sign":
                        expect(within(thisModalRow).getAllByText(sampleRequests[0].callsign)[0]).toBeVisible()
                        break;
                    case "Number of Patients":
                        expect(within(thisModalRow).getAllByText(sampleRequests[0].patientnumber)[0]).toBeVisible()
                        break;
                    case "Precedence":
                        expect(within(thisModalRow).getAllByText(sampleRequests[0].precedence)[0]).toBeVisible()
                        break;
                    case "Special Equipment":
                        expect(within(thisModalRow).getAllByText(sampleRequests[0].specialEquipment.toString())[0]).toBeVisible()
                        break;
                    case "Litter Patient":
                        expect(within(thisModalRow).getAllByText(sampleRequests[0].litterpatient)[0]).toBeVisible()
                        break;
                    case "Ambulatory Patient":
                        expect(within(thisModalRow).getAllByText(sampleRequests[0].ambulatorypatient)[0]).toBeVisible()
                        break;
                    case "Security at Pick-up Site":
                        expect(within(thisModalRow).getAllByText(sampleRequests[0].security)[0]).toBeVisible()
                        break;
                    case "Method of Marking":
                        expect(within(thisModalRow).getAllByText(sampleRequests[0].markingMethod.toString())[0]).toBeVisible()
                        break;
                    case "Patient Nationality and Status":
                        expect(within(thisModalRow).getAllByText(sampleRequests[0].nationality)[0]).toBeVisible()
                        break;
                    case "NBC Contamination":
                        expect(within(thisModalRow).getAllByText(sampleRequests[0].nbc)[0]).toBeVisible()
                        break;
                }
            }

        }
    });
    it('should mark a task complete when the Mark Complete in the detail modal is clicked', async () => {
        getAllSpy.mockResolvedValue(sampleRequests.map(sampleReq => ({...sampleReq, status: "Pending"})));
        postSpy.mockResolvedValue(sampleRequests.map(sampleReq => ({...sampleReq, status: "Complete"})))
        render(<ResponderPage/>);
        userEvent.setup();
        const tableElement = await screen.findByRole('grid');
        const headerRow = screen.getByRole('row');
        await waitFor(() => {
            expect(tableElement).toHaveAttribute('aria-rowcount', '3')
        })
        const allRows = screen.getAllByRole('row').filter(row => row !== headerRow);
        // grab all buttons in the table
        const completeButtons: HTMLElement[] = allRows.map((row, index) => {
            const thisButton = within(row).getByRole('button', {name: "getRequestDetails"});
            expect(thisButton).toHaveTextContent(sampleRequests[index].status);
            return thisButton;
        })
        const firstRow = allRows[0];
        const firstRowButton = completeButtons[0];
        await userEvent.click(firstRowButton);
        const modal = screen.getByRole('dialog')

        const markCompleteButton = within(modal).getByRole("button", {name: "Mark complete"});
        await userEvent.click(markCompleteButton);
        await waitFor(() => {
            expect(markCompleteButton).not.toBeVisible()
        })
        expect(markCompleteButton).not.toBeVisible()
        expect(within(firstRow).getByText('Complete')).toBeVisible();
    });
});