import React, {useEffect, useState} from "react";
import {
    Alert,
    Box,
    Button,
    Checkbox,
    Collapse,
    FormControl,
    FormControlLabel,
    FormGroup,
    FormHelperText,
    FormLabel,
    IconButton,
    InputLabel,
    MenuItem,
    Select,
    Stack,
    TextField,
    Typography
} from "@mui/material";
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import CloseIcon from '@mui/icons-material/Close';
import {useForm} from "react-hook-form";
import {zodResolver} from "@hookform/resolvers/zod";
import * as z from "zod";
import {submitNineLineRequest} from "../services/api.ts";
import {NineLineRequestType} from "../types/NineLineRequestType.ts";


type RequestData = {
    mgrs: string;
    callsign: string;
    patientnumber: number;
    precedence: number;
    litterpatient: number;
    ambulatorypatient: number;
    security: string;
    nationality: number;
    nbc: number;
};

const RequesterPage: React.FunctionComponent = () => {
    const [displaySuccess, setDisplaySuccess] = useState<boolean>(false);
    const [displayError, setDisplayError] = useState<boolean>(false);
    const [specialEquipment, setSpecialEquipment] = useState<boolean[]>([true, false, false, false]);
    const [markingMethods, setMarkingMethods] = useState<boolean[]>([false, false, false, true, false]);

    const {
        handleSubmit,
        register,
        formState: {errors}
    } = useForm<RequestData>({
        resolver: zodResolver(z.object({
            mgrs: z.string()
                .refine((value) => /^\d{1,2}\s?[A-HJ-NP-Z]\s?[A-HJ-NP-Z]{2}\s?((\d{10})|(\d{8})|(\d{5}\s?\d{5})|(\d{4}\s?\d{4}))$/im.test(value ?? ""), "Please enter in correct MGRS format."),
            callsign: z.string().trim().min(1, {message: "Required field."}),
            patientnumber: z.number(),
            precedence: z.number(),
            litterpatient: z.number(),
            ambulatorypatient: z.number(),
            security: z.string(),
            nationality: z.number(),
            nbc: z.number(),
        })),
        defaultValues: {
            patientnumber: 1 as RequestData ['patientnumber'],
            precedence: 1 as RequestData ['precedence'],
            litterpatient: 1 as RequestData ['litterpatient'],
            ambulatorypatient: 0 as RequestData ['ambulatorypatient'],
            security: "N" as RequestData['security'],
            nationality: 1 as RequestData['nationality'],
            nbc: 1 as RequestData['nbc']
        }
    });

    const convertEquipmentCheckbox = () => {
        const list = ["None", "Hoist", "Extraction Equipment", "Ventilator"];
        const ret: string[] = [];

        for (let i = 0; i < specialEquipment.length; i++) {
            if (specialEquipment[i]) ret.push(list[i]);
        }

        if (ret.length < 1) {
            return [list [0]];
        }

        return ret;
    };

    const convertMarkingCheckbox = () => {
        const list = ["Panel", "Pyro", "Smoke", "None", "Other"];
        const ret: string[] = [];

        for (let i = 0; i < markingMethods.length; i++) {
            if (markingMethods[i]) ret.push(list[i]);
        }

        if (ret.length < 1) {
            return [list [3]];
        }

        return ret;
    };

    const onSubmit = (data: RequestData) => {
        const request: NineLineRequestType = {
            id: null,
            location: data.mgrs,
            callsign: data.callsign,
            patientnumber: data.patientnumber,
            precedence: data.precedence,
            specialEquipment: convertEquipmentCheckbox(),
            litterpatient: data.litterpatient,
            ambulatorypatient: data.ambulatorypatient,
            security: data.security,
            markingMethod: convertMarkingCheckbox(),
            nationality: data.nationality,
            nbc: data.nbc,
            status: "Pending"
        };

        submitNineLineRequest(request)
            .then(() => {
                setDisplaySuccess(true);
            })
            .catch(() => {
                setDisplayError(true)
            });
    };

    useEffect(() => {
        if (displaySuccess) {
            (new Promise(resolve => setTimeout(resolve, 3000))).then(() => setDisplaySuccess(false));
        }
    }, [displaySuccess]);

    useEffect(() => {
        if (displayError) {
            (new Promise(resolve => setTimeout(resolve, 3000))).then(() => setDisplayError(false));
        }
    }, [displayError]);

    return (
        <Box>
            <Collapse in={displaySuccess}>
                <Alert
                    aria-label={'successAlert'}
                    iconMapping={{success: <CheckCircleOutlineIcon fontSize="inherit"/>,}}
                    action={
                        <Stack direction={"row"}>
                            <Button id={"viewRequestButton"} name={"viewRequestButton"}
                                    sx={{color: "#113F39"}}
                                    aria-label={"viewSubmittedRequest"}>VIEW REQUEST</Button>
                            <IconButton onClick={() => setDisplaySuccess(false)} id={"closeAlert"} name={"closeAlert"}
                                        aria-label={"closeAlert"}>
                                <CloseIcon/>
                            </IconButton>
                        </Stack>
                    }>
                    Request Submitted. A dispatcher will contact you soon.
                </Alert>
            </Collapse>
            <Collapse in={displayError}>
                <Alert
                    aria-label={'errorAlert'}
                    severity="error"
                    action={
                        <Stack direction={"row"}>
                            <IconButton onClick={() => setDisplayError(false)} id={"closeErrorAlert"}
                                        name={"closeErrorAlert"} aria-label={"closeErrorAlert"}>
                                <CloseIcon/>
                            </IconButton>
                        </Stack>
                    }>
                    Request Denied. A Server error has occurred.
                </Alert>
            </Collapse>
            <Box display="flex" width="100%" justifyContent="center">
                <Box display="flex" maxWidth={"50%"} minWidth={"50%"}>
                    <form onSubmit={handleSubmit(onSubmit)} style={{width: "100%", height: "100%"}}>
                        <Stack direction="column" spacing={3} sx={{my: "3vh"}}>
                            <Typography variant="h3" sx={{textAlign: "left"}}>MEDEVAC Request Form</Typography>
                            <TextField id={"mgrs"}
                                       label={"Location"}
                                       aria-label={"mgrs"}
                                       color={errors.mgrs ? "error" : "success"}
                                       helperText={errors.mgrs ? errors.mgrs.message : "MGRS Format"}
                                       {...register('mgrs')} />
                            <TextField id={"callsign"}
                                       label={"Radio Frequency/ Call Sign/ Suffix"}
                                       aria-label={"callsign"}
                                       color={errors.callsign ? "error" : "success"}
                                       helperText={errors.callsign ? errors.callsign.message : ""}
                                       {...register('callsign')} />
                            <Stack display="flex" sx={{width: "100%"}} direction="row" spacing={1}>
                                <FormControl fullWidth={true} error={errors.patientnumber !== undefined}>
                                    <InputLabel id={"patientnumberlabel"}>Patient Number</InputLabel>
                                    <Select
                                        id={"patientnumber"}
                                        labelId={"patientnumberlabel"}
                                        defaultValue={1}
                                        aria-label={"patientnumber"}
                                        {...register('patientnumber')}
                                    >
                                        <MenuItem aria-label={"1"} value={1}>1</MenuItem>
                                        <MenuItem aria-label={"2"} value={2}>2</MenuItem>
                                        <MenuItem aria-label={"3"} value={3}>3</MenuItem>
                                        <MenuItem aria-label={"4"} value={4}>4</MenuItem>
                                        <MenuItem aria-label={"5"} value={5}>5</MenuItem>
                                        <MenuItem aria-label={"6"} value={6}>6</MenuItem>
                                        <MenuItem aria-label={"7"} value={7}>7</MenuItem>
                                        <MenuItem aria-label={"8"} value={8}>8</MenuItem>
                                        <MenuItem aria-label={"9"} value={9}>9</MenuItem>
                                    </Select>
                                    {errors.patientnumber && (
                                        <FormHelperText>Field Required.</FormHelperText>
                                    )}
                                </FormControl>
                                <FormControl fullWidth={true}>
                                    <InputLabel id={"precedenceLabel"}>Precedence</InputLabel>
                                    <Select
                                        id={"precedence"}
                                        labelId={"precedenceLabel"}
                                        defaultValue={1}
                                        {...register('precedence')}>
                                        <MenuItem aria-label={"Urgent"} value={1}>Urgent</MenuItem>
                                        <MenuItem aria-label={"Not Urgent"} value={2}>Not Urgent</MenuItem>
                                    </Select>
                                </FormControl>
                            </Stack>
                            <FormControl component={"fieldset"} variant={"standard"} aria-label={"Special Equipment"}>
                                <FormLabel component={"legend"}>Special Equipment</FormLabel>
                                <FormGroup>
                                    <FormControlLabel label={"None"}
                                                      aria-label={"None"}
                                                      control={<Checkbox name={"equipmentNone"}
                                                                         checked={specialEquipment[0]}
                                                                         onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
                                                                             setSpecialEquipment([event.target.checked, false, false, false]);
                                                                         }}/>}/>
                                    <FormControlLabel label={"Hoist"}
                                                      aria-label={"Hoist"}
                                                      control={<Checkbox name={"equipmentHoist"}
                                                                         checked={specialEquipment[1]}
                                                                         onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
                                                                             setSpecialEquipment([false, event.target.checked, specialEquipment[2], specialEquipment[3]]);
                                                                         }}/>}/>
                                    <FormControlLabel label={"Extraction Equipment"}
                                                      aria-label={"Extraction Equipment"}
                                                      control={<Checkbox name={"equipmentExtractionEquipment"}
                                                                         checked={specialEquipment[2]}
                                                                         onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
                                                                             setSpecialEquipment([false, specialEquipment[1], event.target.checked, specialEquipment[3]]);
                                                                         }}/>}/>
                                    <FormControlLabel label={"Ventilator"}
                                                      aria-label={"Ventilator"}
                                                      control={<Checkbox name={"equipmentVentilator"}
                                                                         checked={specialEquipment[3]}
                                                                         onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
                                                                             setSpecialEquipment([false, specialEquipment[1], specialEquipment[2], event.target.checked]);
                                                                         }}/>}/>
                                </FormGroup>
                            </FormControl>
                            <Stack display="flex" sx={{width: "100%"}} direction="row" spacing={1}>
                                <FormControl fullWidth={true} error={errors.litterpatient !== undefined}>
                                    <InputLabel id={"litterpatientlabel"}>Litter Patient #</InputLabel>
                                    <Select
                                        id={"litterpatient"}
                                        labelId={"litterpatientlabel"}
                                        aria-label={"litterpatient"}
                                        defaultValue={1}
                                        {...register('litterpatient')}
                                    >
                                        <MenuItem aria-label={"0"} value={0}>0</MenuItem>
                                        <MenuItem aria-label={"1"} value={1}>1</MenuItem>
                                        <MenuItem aria-label={"2"} value={2}>2</MenuItem>
                                        <MenuItem aria-label={"3"} value={3}>3</MenuItem>
                                        <MenuItem aria-label={"4"} value={4}>4</MenuItem>
                                        <MenuItem aria-label={"5"} value={5}>5</MenuItem>
                                        <MenuItem aria-label={"6"} value={6}>6</MenuItem>
                                        <MenuItem aria-label={"7"} value={7}>7</MenuItem>
                                        <MenuItem aria-label={"8"} value={8}>8</MenuItem>
                                        <MenuItem aria-label={"9"} value={9}>9</MenuItem>
                                    </Select>
                                    {errors.litterpatient && (
                                        <FormHelperText>Field Required.</FormHelperText>
                                    )}
                                </FormControl>
                                <FormControl fullWidth={true} error={errors.ambulatorypatient !== undefined}>
                                    <InputLabel id={"ambulatorypatientlabel"}>Ambulatory Patient #</InputLabel>
                                    <Select
                                        id={"ambulatorypatient"}
                                        labelId={"ambulatorypatientlabel"}
                                        aria-label={"ambulatorypatient"}
                                        defaultValue={0}
                                        {...register('ambulatorypatient')}>
                                        <MenuItem aria-label={"0"} value={0}>0</MenuItem>
                                        <MenuItem aria-label={"1"} value={1}>1</MenuItem>
                                        <MenuItem aria-label={"2"} value={2}>2</MenuItem>
                                        <MenuItem aria-label={"3"} value={3}>3</MenuItem>
                                        <MenuItem aria-label={"4"} value={4}>4</MenuItem>
                                        <MenuItem aria-label={"5"} value={5}>5</MenuItem>
                                        <MenuItem aria-label={"6"} value={6}>6</MenuItem>
                                        <MenuItem aria-label={"7"} value={7}>7</MenuItem>
                                        <MenuItem aria-label={"8"} value={8}>8</MenuItem>
                                        <MenuItem aria-label={"9"} value={9}>9</MenuItem>
                                    </Select>
                                </FormControl>
                            </Stack>
                            <FormControl error={errors.security !== undefined}>
                                <InputLabel id={"securitylabel"}>Security at Pick-Up Site</InputLabel>
                                <Select
                                    id={"security"}
                                    labelId={"securitylabel"}
                                    aria-label={"security"}
                                    defaultValue={"N"}
                                    {...register('security')}>
                                    <MenuItem aria-label={"N"} value={"N"}>No enemies in the area</MenuItem>
                                    <MenuItem aria-label={"P"} value={"P"}>Potential enemy in the area</MenuItem>
                                    <MenuItem aria-label={"E"} value={"E"}>Enemy troops in the area, proceed with
                                        caution</MenuItem>
                                    <MenuItem aria-label={"X"} value={"X"}>Confirmed enemy in the area, armed escort
                                        required</MenuItem>
                                </Select>
                            </FormControl>
                            <FormControl component={"fieldset"} variant={"standard"} aria-label={"Marking Methods"}>
                                <FormLabel component={"legend"}>Method of Marking Pick-up Site</FormLabel>
                                <FormGroup>
                                    <FormControlLabel label={"Panel"}
                                                      aria-label={"Panel"}
                                                      control={<Checkbox name={"Panel"}
                                                                         checked={markingMethods[0]}
                                                                         onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
                                                                             setMarkingMethods([event.target.checked, markingMethods[1], markingMethods[2], false, markingMethods[4]]);
                                                                         }}/>}/>
                                    <FormControlLabel label={"Pyrotechnic signal"}
                                                      aria-label={"Pyro"}
                                                      control={<Checkbox name={"Pyro"}
                                                                         checked={markingMethods[1]}
                                                                         onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
                                                                             setMarkingMethods([markingMethods[0], event.target.checked, markingMethods[2], false, markingMethods[4]]);
                                                                         }}/>}/>
                                    <FormControlLabel label={"Smoke Signal"}
                                                      aria-label={"Smoke"}
                                                      control={<Checkbox name={"Smoke"}
                                                                         checked={markingMethods[2]}
                                                                         onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
                                                                             setMarkingMethods([markingMethods[0], markingMethods[1], event.target.checked, false, markingMethods[4]]);
                                                                         }}/>}/>
                                    <FormControlLabel label={"None"}
                                                      aria-label={"None"}
                                                      control={<Checkbox name={"None"}
                                                                         checked={markingMethods[3]}
                                                                         onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
                                                                             setMarkingMethods([false, false, false, event.target.checked, false]);
                                                                         }}/>}/>
                                    <FormControlLabel label={"Other"}
                                                      aria-label={"Other"}
                                                      control={<Checkbox name={"Other"}
                                                                         checked={markingMethods[4]}
                                                                         onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
                                                                             setMarkingMethods([markingMethods[0], markingMethods[1], markingMethods[2], false, event.target.checked]);
                                                                         }}/>}/>
                                </FormGroup>
                            </FormControl>
                            <FormControl error={errors.security !== undefined}>
                                <InputLabel id={"nationalityLabel"}>Patient Nationality and Status</InputLabel>
                                <Select
                                    id={"nationality"}
                                    labelId={"nationalityLabel"}
                                    aria-label={"nationality"}
                                    defaultValue={1}
                                    {...register('nationality')}>
                                    <MenuItem aria-label={"USM"} value={1}>U.S. Military</MenuItem>
                                    <MenuItem aria-label={"USC"} value={2}>U.S. Civilian</MenuItem>
                                    <MenuItem aria-label={"NUSM"} value={3}>Non-U.S. Military</MenuItem>
                                    <MenuItem aria-label={"NUSC"} value={4}>Non-U.S. Civilian</MenuItem>
                                    <MenuItem aria-label={"EPW"} value={5}>EPW</MenuItem>
                                </Select>
                            </FormControl>
                            <FormControl error={errors.security !== undefined}>
                                <InputLabel id={"nbcLabel"}>NBC Contamination</InputLabel>
                                <Select
                                    id={"nbc"}
                                    labelId={"nbcLabel"}
                                    aria-label={"nbc"}
                                    defaultValue={1}
                                    {...register('nbc')}>
                                    <MenuItem aria-label={'Nuclear'} value={1}>Nuclear</MenuItem>
                                    <MenuItem aria-label={'Biological'} value={2}>Biological</MenuItem>
                                    <MenuItem aria-label={'Chemical'} value={3}>Chemical</MenuItem>
                                    <MenuItem aria-label={'None'} value={4}>None</MenuItem>
                                </Select>
                            </FormControl>
                            <Button id={"submit"} type={"submit"} variant={"contained"}
                                    aria-label={"Submit"}
                                    sx={{width: "15%", display: "block", alignSelf: "flex-end"}}>Submit</Button>
                        </Stack>
                    </form>
                </Box>
            </Box>
        </Box>
    );
};

export default RequesterPage;