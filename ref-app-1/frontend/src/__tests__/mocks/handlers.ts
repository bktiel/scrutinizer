import {NineLineRequestType} from "../../types/NineLineRequestType.ts";

export const sampleResponse: NineLineRequestType = {
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
    status: "Complete",
};

export const sampleResponses = [{...sampleResponse, id: 1}, {...sampleResponse, id: 2}];

export const handlers = []