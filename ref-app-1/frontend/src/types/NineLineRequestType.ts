export type NineLineRequestType = {
    id?: number | null;
    location: string;
    callsign: string;
    patientnumber: number;
    precedence: number;
    specialEquipment: string [];
    litterpatient: number;
    ambulatorypatient: number;
    security: string;
    markingMethod: string [];
    nationality: number;
    status: string;
    nbc: number;
};

export const nationalities: string [] = ["USM", "USC", "NUSM", "NUSC", "EPW"];
export const nbcs: string [] = ["Nuclear", "Biological", "Chemical"];