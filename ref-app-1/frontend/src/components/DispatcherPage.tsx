import * as React from "react";
import {useEffect, useState} from "react";
import {
    Alert,
    Box,
    Checkbox,
    Collapse,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControl,
    IconButton,
    InputLabel,
    MenuItem,
    Paper,
    Select,
    SelectChangeEvent,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Tooltip,
    Typography
} from "@mui/material";
import {NineLineRequestType} from "../types/NineLineRequestType.ts";
import {getAllNineLineRequests, getAssignments, getResponders, postRequestAssignment} from "../services/api.ts";
import Toolbar from "@mui/material/Toolbar";
import Button from "@mui/material/Button";
import CloseIcon from "@mui/icons-material/Close";
import {RequestAssignmentType} from "../types/RequestAssignmentType.ts";
import Container from "@mui/material/Container";
import {ResponderType} from "../types/ResponderType.ts";

const DispatcherPage = () => {
    const [error, setError] = useState<string>();
    const [success, setSuccess] = useState<boolean>(false);
    const [selected, setSelected] = useState<number []>([]);
    const [requests, setRequests] = useState<NineLineRequestType []>();
    const [assignee, setAssignee] = useState<number>(1);
    const [open, setOpen] = React.useState(false);
    const [openRequest, setOpenRequest] = useState<NineLineRequestType>();
    const [responders, setResponders] = useState<ResponderType[]>([])

    useEffect(() => {
        getResponders().then(resp=>{
            setResponders(resp)
        })
    }, [requests,openRequest]);

    const headCells = [
        {
            label: 'Location',
        },
        {
            label: 'Call Sign',
        },
        {
            label: 'Precedence',
        },
        {
            label: 'Special Equipment',
        },
        {
            label: 'Security',
        },
        {
            label: 'Marking',
        },
        {
            label: 'Details',
        },
    ];

    const handleClickOpen = () => {
        setOpen(true);
    };

    const handleClose = () => {
        setOpen(false);
    };

    const loadAndFilterRequests = async () => {
        const requests = await getAllNineLineRequests();
        const assignments = await getAssignments();

        if (assignments.length) {
            return requests.filter((request: NineLineRequestType) =>
                assignments.find((assignment: RequestAssignmentType) =>
                    assignment.requestId === request.id) === undefined);
        } else {
            return requests;
        }
    };

    const handleAssign = () => {
        if (!requests) {
            return;
        }

        if (!assignee) {
            return;
        }

        const assignments = selected.map((index: number) => ({
            requestId: requests [index].id,
            responder: assignee,
        } as RequestAssignmentType));

        postRequestAssignment(assignments)
            .then(() => {
                setOpen(false);
                setSuccess(true);
                loadAndFilterRequests().then((requests: NineLineRequestType []) => setRequests(requests));
            })
            .catch((error) => {
                console.log(error)
                setError("Failed to save assignment.");
            });
    };

    useEffect(() => {
        loadAndFilterRequests().then((requests: NineLineRequestType []) => setRequests(requests));
    }, []);

    useEffect(() => {
        if (success) {
            (new Promise(resolve => setTimeout(resolve, 3000))).then(() => setSuccess(false));
        }
    }, [success]);

    useEffect(() => {
        if (error) {
            (new Promise(resolve => setTimeout(resolve, 5000))).then(() => setError(undefined));
        }
    }, [error]);

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

    // const ALL_RESPONDERS = [
    //     "Red",
    //     "Blue",
    //     "Yellow"
    // ]

    return (
        <>
            <Container>
                <Box sx={{height: "4em"}}>
                    <Collapse in={success}>
                        <Alert severity={"success"}>
                            Assigned to {responders.find(responder=>responder.id===assignee)?.callsign}
                        </Alert>
                    </Collapse>
                    <Collapse in={error !== undefined}>
                        <Alert severity={"error"}>
                            {error}
                        </Alert>
                    </Collapse>
                </Box>
                <Typography variant={"h3"}>
                    Outstanding MEDEVAC Requests
                </Typography>
                <Box sx={{height: "3em"}}></Box>
                <Paper sx={{width: '100%'}}>
                    <Toolbar aria-label={"tableTitle"} sx={selected.length === 0 ? {backgroundColor: '#FFE47A'} : {}}>
                        {selected.length > 0 ? (
                            <>
                                <Typography sx={{flex: '1 1 100%'}} color={"inherit"} variant={"subtitle1"}
                                            component={"div"}>
                                    {selected.length} selected
                                </Typography>
                                <Tooltip title={"Assign"}>
                                    <Button variant={"outlined"} id={"assignButton"}
                                            aria-label={"assignButton"}
                                            onClick={handleClickOpen}>Assign</Button>
                                </Tooltip>
                            </>
                        ) : (
                            <Typography variant={"h6"} id={"tableTitle"} component={"div"}>
                                Requests
                            </Typography>
                        )}
                    </Toolbar>
                    <TableContainer>
                        <Table aria-rowcount={requests ? requests.length : 0}>
                            <TableHead>
                                <TableRow>
                                    <TableCell padding={"checkbox"}>
                                        <Checkbox checked={requests && selected.length === requests.length}
                                                  indeterminate={requests && selected.length > 0 && selected.length < requests.length}
                                                  aria-label={"checkboxAll"}
                                                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => (requests && setSelected(e.target.checked ? requests.map((_: NineLineRequestType, id: number) => id) : []))}/>
                                    </TableCell>
                                    {headCells.map((headCell) => (
                                        <TableCell key={headCell.label} align={'right'}>
                                            {headCell.label}
                                        </TableCell>))}
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {requests && (
                                    requests.map((request: NineLineRequestType, index: number) => (
                                        <TableRow key={index}>
                                            <TableCell padding="checkbox">
                                                <Checkbox
                                                    checked={selected.indexOf(index) !== -1}
                                                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSelected(e.target.checked ? [...selected, index] : selected.filter((value: number) => value !== index))}
                                                />
                                            </TableCell>
                                            <TableCell
                                                component="th"
                                                id={request.location}
                                                scope="row"
                                                padding="none"
                                            >
                                                {request.location}
                                            </TableCell>
                                            <TableCell align="right">{request.callsign}</TableCell>
                                            <TableCell
                                                align="right">{precedenceDictionary[request.precedence]}</TableCell>
                                            <TableCell align="right">{request.specialEquipment.join(", ")}</TableCell>
                                            <TableCell align="right">{securityDictionary[request.security]}</TableCell>
                                            <TableCell align="right">{request.markingMethod.join(", ")}</TableCell>
                                            <TableCell align="right">
                                                <Button aria-label="viewDialog" onClick={() => {
                                                    setSelected([index]);
                                                    setOpenRequest(request);
                                                }}>View</Button>
                                            </TableCell>
                                        </TableRow>
                                    ))
                                )}
                            </TableBody>
                        </Table>
                    </TableContainer>
                </Paper>
            </Container>
            <Dialog onClose={handleClose} open={open} maxWidth={"md"} fullWidth={true}>
                <DialogTitle sx={{m: 0, p: 2}} id="customized-dialog-title">
                    MEDEVAC Request
                </DialogTitle>
                <IconButton
                    aria-label="close"
                    onClick={handleClose}
                    sx={{
                        position: 'absolute',
                        right: 8,
                        top: 8,
                        color: (theme) => theme.palette.grey[500],
                    }}>
                    <CloseIcon/>
                </IconButton>
                <DialogContent dividers>
                    <Container sx={{display: "flex", flexDirection: "column", justifyContent: "center", width: "40%"}}>
                        <FormControl>
                            <InputLabel id={"responderLabel"}>Responder</InputLabel>
                            <Select
                                id={"responder"}
                                labelId={"responderLabel"}
                                value={assignee}
                                onChange={(e: SelectChangeEvent<number>) => setAssignee(e.target.value as number)}>
                                {
                                    responders.map((responder, index) => (
                                        <MenuItem aria-label={responder.callsign} value={index + 1}>{responder.callsign}</MenuItem>
                                    ))
                                }
                            </Select>
                        </FormControl>
                    </Container>
                </DialogContent>
                <DialogActions>
                    <Button variant="contained" autoFocus onClick={handleAssign}>
                        Assign
                    </Button>
                    <Button onClick={handleClose}>
                        Cancel
                    </Button>
                </DialogActions>
            </Dialog>
            <Dialog onClose={() => setOpenRequest(undefined)} open={openRequest !== undefined} maxWidth={"md"}
                    fullWidth={true}>
                <DialogTitle sx={{m: 0, p: 2}} id="customized-dialog-title">
                    MEDEVAC Request
                </DialogTitle>
                <IconButton
                    aria-label="close"
                    onClick={() => setOpenRequest(undefined)}
                    sx={{
                        position: 'absolute',
                        right: 8,
                        top: 8,
                        color: (theme) => theme.palette.grey[500],
                    }}>
                    <CloseIcon/>
                </IconButton>
                <DialogContent dividers>
                    <Container>
                        <Stack spacing={1} alignItems={"center"}>
                            <Stack direction={"row"} width={"100%"} spacing={2}>
                                <Typography fontWeight={"bold"} width={"50%"} textAlign={"end"}>Location</Typography>
                                <Typography>{openRequest && openRequest.location}</Typography>
                            </Stack>
                            <Stack direction={"row"} width={"100%"} spacing={2}>
                                <Typography fontWeight={"bold"} width={"50%"} textAlign={"end"}>Call Sign</Typography>
                                <Typography>{openRequest && openRequest.callsign}</Typography>
                            </Stack>
                            <Stack direction={"row"} width={"100%"} spacing={2}>
                                <Typography fontWeight={"bold"} width={"50%"} textAlign={"end"}>Number of
                                    Patients</Typography>
                                <Typography>{openRequest && openRequest.patientnumber}</Typography>
                            </Stack>
                            <Stack direction={"row"} width={"100%"} spacing={2}>
                                <Typography fontWeight={"bold"} width={"50%"} textAlign={"end"}>Precedence</Typography>
                                <Typography>{openRequest && precedenceDictionary[openRequest.precedence]}</Typography>
                            </Stack>
                            <Stack direction={"row"} width={"100%"} spacing={2}>
                                <Typography fontWeight={"bold"} width={"50%"} textAlign={"end"}>Special
                                    Equipment</Typography>
                                <Typography>{openRequest && openRequest.specialEquipment.join(", ")}</Typography>
                            </Stack>
                            <Stack direction={"row"} width={"100%"} spacing={2}>
                                <Typography fontWeight={"bold"} width={"50%"} textAlign={"end"}>Litter
                                    Patient</Typography>
                                <Typography>{openRequest && openRequest.litterpatient}</Typography>
                            </Stack>
                            <Stack direction={"row"} width={"100%"} spacing={2}>
                                <Typography fontWeight={"bold"} width={"50%"} textAlign={"end"}>Ambulatory
                                    Patient</Typography>
                                <Typography>{openRequest && openRequest.ambulatorypatient}</Typography>
                            </Stack>
                            <Stack direction={"row"} width={"100%"} spacing={2}>
                                <Typography fontWeight={"bold"} width={"50%"} textAlign={"end"}>Security at Pick-up
                                    Site</Typography>
                                <Typography>{openRequest && securityDictionary[openRequest.security]}</Typography>
                            </Stack>
                            <Stack direction={"row"} width={"100%"} spacing={2}>
                                <Typography fontWeight={"bold"} width={"50%"} textAlign={"end"}>Method of
                                    Marking</Typography>
                                <Typography>{openRequest && openRequest.markingMethod.join(", ")}</Typography>
                            </Stack>
                            <Stack direction={"row"} width={"100%"} spacing={2}>
                                <Typography fontWeight={"bold"} width={"50%"} textAlign={"end"}>Patient Nationality and
                                    Status</Typography>
                                <Typography>{openRequest && nationalities[openRequest.nationality]}</Typography>
                            </Stack>
                            <Stack direction={"row"} width={"100%"} spacing={2}>
                                <Typography fontWeight={"bold"} width={"50%"} textAlign={"end"}>NBC
                                    Contamination</Typography>
                                <Typography>{openRequest && nbcs[openRequest.nbc]}</Typography>
                            </Stack>
                            <Stack direction={"row"} width={"100%"} spacing={2}>
                                <Box sx={{width: "50%"}}>
                                </Box>
                                <FormControl sx={{width: "40%"}}>
                                    <InputLabel id={"responderLabel"}>Responder</InputLabel>
                                    <Select
                                        id={"responder"}
                                        labelId={"responderLabel"}
                                        value={assignee}
                                        onChange={(e: SelectChangeEvent<number>) => setAssignee(e.target.value as number)}>
                                        {
                                            responders.map((responder) => (
                                                <MenuItem aria-label={responder.callsign}
                                                          value={responder.id}>{responder.callsign}</MenuItem>
                                            ))
                                        }
                                    </Select>
                                </FormControl>
                            </Stack>
                        </Stack>
                    </Container>
                </DialogContent>
                <DialogActions>
                    <Button variant="contained" autoFocus aria-label={"viewAssignButton"} onClick={() => {
                        handleAssign();
                        setOpenRequest(undefined);
                    }}>
                        Assign
                    </Button>
                    <Button onClick={() => {
                        setOpenRequest(undefined);
                    }}>
                        Cancel
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    );
};

export default DispatcherPage;