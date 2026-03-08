describe('responder page', () => {
    beforeEach(() => {
        cy.session('responderSession', () => {
                cy.authenticateAsResponder()
            },
        );
    })
   it('should go to responder page', () => {
       cy.goToResponderPage();
       cy.findByText(/medevac assignment/i).should('be.visible');
   })

    it('should check first item and display number of selected items, a check button, and a delete button.', () => {
        cy.goToResponderPage();
        cy.get('.MuiDataGrid-row--firstVisible > .MuiDataGrid-cellCheckbox > .MuiButtonBase-root > .PrivateSwitchBase-input').click();
        cy.findByText(/1 Selected/i).should('be.visible');
        cy.get('[aria-label="setRequestsCompleted"]').should('be.visible');
        cy.get('[aria-label="setRequestsDeleted"]').should('be.visible');
    })

    it('should mark the first row complete when the check box button is clicked.', () => {
        cy.goToResponderPage();
        cy.get('.MuiDataGrid-row--firstVisible > .MuiDataGrid-cellCheckbox > .MuiButtonBase-root > .PrivateSwitchBase-input').click();
        cy.findByText(/1 Selected/i).should('be.visible');
        cy.get('[aria-label="setRequestsCompleted"]').click();
        cy.findByText(/request completed/i).should('be.visible');
        cy.get('.MuiDataGrid-row--firstVisible > [data-field="status"]').contains(/complete/i)
    })

    it('should mark complete under the status dialog and ensure the row status says complete', () => {
        cy.goToResponderPage();
        cy.get('.MuiDataGrid-row--firstVisible > [data-field="status"] > .MuiButtonBase-root').click();
        cy.findByText(/mark complete/i).should('be.visible')
        cy.get('.MuiDialogActions-root > .MuiButtonBase-root').click();
        cy.get('.MuiDialogActions-root > .MuiButtonBase-root').contains(/complete/i)
    })
});