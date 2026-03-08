describe('template spec', () => {
    beforeEach(() => {
        cy.session('dispatchSession', () => {
                cy.authenticateAsSuper()
            },
        )
    })
    it('goes to dispatcher page and sees dispatcher page header', () => {
        // cy.authenticateAsSuper();
        cy.goToDispatcherPage();
        cy.findByText(/Outstanding MEDEVAC Requests/i).should('be.visible');
    })
    it('should navigate to the dispatcher page and show pending requests', () => {
        for (let i = 0; i < 5; i++) {
            cy.login()
            cy.createRequest();
        }

        cy.goToDispatcherPage();
        cy.findAllByText(`922929/Arrow/6`).should('have.length.greaterThan', 4);
    });
    it('should be able to click on a single request and assign a responder', () => {
        const targetResponder = "BMan_4"
        cy.login()
        cy.createRequest();
        // cy.authenticateAsSuper();
        cy.goToDispatcherPage();
        let requestCountBeforeAssignment = 0;
        cy.findAllByText(`922929/Arrow/6`).then(($elements) => {
            requestCountBeforeAssignment = $elements.length
            cy.get(':nth-child(1) > :nth-child(8) > .MuiButtonBase-root').click()
                .get('#responder').click()
                .get(`[aria-label="${targetResponder}"]`).click()
                .get('.MuiDialogActions-root > .MuiButton-contained').click()
            cy.findByRole('alert').within(() => {
                cy.findByText(new RegExp(`Assigned to ${targetResponder}`, "i")).should('be.visible')
            })
            cy.findAllByText('922929/Arrow/6').should('have.length', requestCountBeforeAssignment - 1)
        })
    });
    it('should be able to select multiple request and assign a responder', () => {

        const targetResponder = "BMan_4"

        for (let i = 0; i < 3; i++) {
            cy.login()
            cy.createRequest();
        }
        // cy.authenticateAsSuper();

        cy.goToDispatcherPage();
        for (let i = 1; i < 4; i++) {
            cy.get(`.MuiTableBody-root > :nth-child(${i}) > .MuiTableCell-paddingCheckbox > .MuiButtonBase-root > .PrivateSwitchBase-input`).click()
        }
        cy.get('#assignButton').click()
        cy.get('#responder').click()
        cy.get(`[aria-label="${targetResponder}"]`).click()
        cy.get('.MuiDialogActions-root > .MuiButton-contained').click()
        cy.findByRole('alert').within(() => {
            cy.findByText(new RegExp(`Assigned to ${targetResponder}`, "i")).should('be.visible')
        })
    });
})

