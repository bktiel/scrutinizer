import * as React from "react";
import {useEffect, useState} from "react";
import {
    Alert,
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    IconButton,
    Stack,
    Typography
} from "@mui/material";
import {
    DataGrid,
    GridCallbackDetails,
    GridColDef,
    GridRenderCellParams,
    GridRowModel,
    GridRowSelectionModel,
    useGridApiRef
} from '@mui/x-data-grid';
import {NineLineRequestType} from "../types/NineLineRequestType.ts";
import {getAssignedNineLineRequests, updateNineLineRequests} from "../services/api.ts";
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import DeleteForeverIcon from "@mui/icons-material/DeleteForever";
import CloseIcon from "@mui/icons-material/Close";

export type ResponderPageProps = {}

const Modal = (props: {
    handleClose: (() => void) | undefined,
    open: boolean,
    requestDetail: (NineLineRequestType),
    handleMarkComplete: (() => void) | undefined
}) => {
    return (
        <Dialog
            onClose={props.handleClose}
            aria-labelledby="customized-dialog-title"
            open={props.open}
            maxWidth="md"
            fullWidth={true}
        >
            <DialogTitle sx={{m: 0, p: 2}} id="customized-dialog-title">
                MEDEVAC Request
            </DialogTitle>
            <IconButton
                aria-label="close"
                onClick={props.handleClose}
                sx={{
                    position: 'absolute',
                    right: 8,
                    top: 8,
                    color: (theme) => theme.palette.grey[500],
                }}
            >
                <CloseIcon/>
            </IconButton>
            <DialogContent dividers>
                <Stack spacing={1} alignItems="center">
                    <Stack direction={"row"} width={"100%"} spacing={1}>
                        <Typography fontWeight="bold" width="50%" textAlign="end">Status</Typography>
                        <Typography>{props.requestDetail.status}</Typography>
                    </Stack>
                    <Stack direction={"row"} width={"100%"} spacing={1}>
                        <Typography fontWeight="bold" width="50%" textAlign="end">Location</Typography>
                        <Typography>{props.requestDetail.location}</Typography>
                    </Stack>
                    <Stack direction={"row"} width={"100%"} spacing={1}>
                        <Typography fontWeight="bold" width="50%" textAlign="end">Call Sign</Typography>
                        <Typography>{props.requestDetail.callsign}</Typography>
                    </Stack>
                    <Stack direction={"row"} width={"100%"} spacing={1}>
                        <Typography fontWeight="bold" width="50%" textAlign="end">Number of Patients</Typography>
                        <Typography>{props.requestDetail.patientnumber}</Typography>
                    </Stack>
                    <Stack direction={"row"} width={"100%"} spacing={1}>
                        <Typography fontWeight="bold" width="50%" textAlign="end">Precedence</Typography>
                        <Typography>{props.requestDetail.precedence}</Typography>
                    </Stack>
                    <Stack direction={"row"} width={"100%"} spacing={1}>
                        <Typography fontWeight="bold" width="50%" textAlign="end">Special Equipment</Typography>
                        <Typography>{`[${props.requestDetail.specialEquipment.join()}]`}</Typography>
                    </Stack>
                    <Stack direction={"row"} width={"100%"} spacing={1}>
                        <Typography fontWeight="bold" width="50%" textAlign="end">Litter Patient</Typography>
                        <Typography>{props.requestDetail.litterpatient}</Typography>
                    </Stack>
                    <Stack direction={"row"} width={"100%"} spacing={1}>
                        <Typography fontWeight="bold" width="50%" textAlign="end">Ambulatory Patient</Typography>
                        <Typography>{props.requestDetail.ambulatorypatient}</Typography>
                    </Stack>
                    <Stack direction={"row"} width={"100%"} spacing={1}>
                        <Typography fontWeight="bold" width="50%" textAlign="end">Security at Pick-up Site</Typography>
                        <Typography>{props.requestDetail.security}</Typography>
                    </Stack>
                    <Stack direction={"row"} width={"100%"} spacing={1}>
                        <Typography fontWeight="bold" width="50%" textAlign="end">Method of Marking</Typography>
                        <Typography>{`[${props.requestDetail.markingMethod.join()}]`}</Typography>
                    </Stack>
                    <Stack direction={"row"} width={"100%"} spacing={1}>
                        <Typography fontWeight="bold" width="50%" textAlign="end">Patient Nationality and
                            status</Typography>
                        <Typography>{props.requestDetail.nationality}</Typography>
                    </Stack>
                    <Stack direction={"row"} width={"100%"} spacing={1}>
                        <Typography fontWeight="bold" width="50%" textAlign="end">NBC Contamination</Typography>
                        <Typography>{props.requestDetail.nbc}</Typography>
                    </Stack>
                </Stack>
                <Typography gutterBottom sx={{marginY: "1em"}}>
                </Typography>
            </DialogContent>
            <DialogActions>
                <Button autoFocus aria-label={"Mark complete"} variant="contained" onClick={() => {
                    props.handleMarkComplete && props.handleMarkComplete()
                    props.handleClose && props.handleClose()
                }}>
                    Mark Complete
                </Button>
            </DialogActions>
        </Dialog>
    )
}

const ResponderPage: React.FunctionComponent<ResponderPageProps> = () => {

    const columns: GridColDef<(typeof rows)[number]>[] = [
        {
            field: 'precedence',
            headerName: 'Precedence',
            width: 150,
            sortable: true,
        },
        {
            field: 'location',
            headerName: 'Location',
            width: 150,
            sortable: true,
        },
        {
            field: 'callsign',
            headerName: 'Call Sign',
            width: 150,
            sortable: true,
        },
        {
            field: 'security',
            headerName: 'Security',
            width: 150,
            sortable: true,
        },
        {
            field: 'marking',
            headerName: 'Marking',
            width: 150,
            sortable: true,
            valueGetter: (_, index) => {
                return index.markingMethod
            }
        },
        {
            field: 'specialequipment',
            headerName: 'Special Equipment',
            width: 150,
            sortable: true,
            valueGetter: (_, index) => {
                return index.specialEquipment
            }
        },
        {
            field: 'status',
            headerName: 'Status',
            width: 150,
            sortable: true,
            renderCell: (params: GridRenderCellParams<NineLineRequestType>) => (
                <Button
                    size="small"
                    style={{marginLeft: 16}}
                    tabIndex={params.hasFocus ? 0 : -1}
                    onClick={handleClickOpen.bind(this, params.row)}
                    aria-label={"getRequestDetails"}
                >
                    {params.value}
                </Button>
            ),
        },
    ]

    const [rows, setRows] = useState<NineLineRequestType[]>([])
    const [selected, setSelected] = useState<NineLineRequestType[]>([])
    const [recentlyUpdatedRequests, setRecentlyUpdatedRequests] = useState<NineLineRequestType[]>([])
    const [requestToGiveDetail, setRequestToGiveDetail] = useState<NineLineRequestType>()
    const gridApiRef = useGridApiRef();

    const [open, setOpen] = React.useState(false);

    const handleClickOpen = (rowDetail: GridRowModel) => {
        const requestToGiveDetail = rows.find(row => row.id === rowDetail.id);
        if (requestToGiveDetail) {
            setRequestToGiveDetail(requestToGiveDetail);
            setOpen(true);
        }
    };
    const handleClose = () => {
        setOpen(false);
    };

    useEffect(() => {
        getAssignedNineLineRequests().then(res => {
            setRows(res);
        })
    }, []);

    const rowClickHandler = (ids: GridRowSelectionModel, _details: GridCallbackDetails<any>) => {
        const selectedRequests = rows.filter(row => ids.includes(row.id as number))
        // const selectedRequests = ids.map(id => rows.find(row => row.id === id));
        setSelected(selectedRequests)
    }

    const handleModalComplete = () => {
        if (requestToGiveDetail) {
            updateNineLineRequests([requestToGiveDetail]).then(response => {
                setRows(rows.map(r => {
                    const updatedRow = response.find(upRow => upRow.id === r.id)
                    return (updatedRow) ? updatedRow : r;
                }))
            })
        }
    }

    const handleComplete = () => {
        setRecentlyUpdatedRequests(selected);
        gridApiRef.current.setRowSelectionModel([])
        updateNineLineRequests(selected).then(response => {
            setRows(rows.map(r => {
                const updatedRow = response.find(upRow => upRow.id === r.id)
                return (updatedRow) ? updatedRow : r;
            }))
        })
        setTimeout(() => {
            setRecentlyUpdatedRequests([]);
        }, 3000);
    }

    return (
        <Stack flex="true" direction="column" justifyContent="center">
            <Box sx={{height: "4em"}}>
                {recentlyUpdatedRequests.length > 0 &&
                    <Alert severity="success">
                        {`${recentlyUpdatedRequests.length} Request Completed`}
                    </Alert>}
            </Box>

            <Stack flex="true" maxWidth="90%" direction="column" alignSelf="center" spacing={1}>
                <Box flex="true">
                    <Typography variant="h3">MEDEVAC Assignment</Typography>
                </Box>
                <Box sx={{height: "3em"}}/>
                <Box>
                    <Box display="flex" flexDirection="row"
                         sx={{height: '4em', width: '100%'}}>
                        {
                            selected.length > 0 ?
                                <Stack direction={'row'} alignSelf="center" display="flex"
                                       alignContent="center" width="100%" height="100%"
                                       sx={{backgroundColor: "#FFE47A"}}
                                >
                                    <Box display="flex"
                                         alignItems="center"
                                         minWidth="15%"
                                         justifyContent="center"
                                         maxHeight="true"
                                         textAlign="center">
                                        <Typography>{`${selected.length} Selected`}</Typography>
                                    </Box>
                                    <Stack direction="row" width="100%" justifyContent="flex-end" marginRight="2em">
                                        <Box display="flex"
                                             minWidth="10%"
                                             alignItems="center">
                                            <IconButton aria-label="setRequestsCompleted" onClick={handleComplete}>
                                                <CheckCircleIcon fontSize="large"/>
                                            </IconButton>
                                            <IconButton aria-label="setRequestsDeleted">
                                                <DeleteForeverIcon fontSize="large"/>
                                            </IconButton>
                                        </Box>
                                    </Stack>
                                </Stack> :
                                <Typography variant="h4">Requests</Typography>
                        }
                    </Box>
                    <Box sx={{minHeight: "5em"}}>
                        <DataGrid
                            sx={{height: "100%"}}
                            rows={rows}
                            onRowSelectionModelChange={rowClickHandler}
                            apiRef={gridApiRef}
                            autoHeight={true}
                            columns={columns}
                            initialState={{
                                pagination: {
                                    paginationModel: {
                                        pageSize: 5,
                                    },
                                },
                            }}
                            pageSizeOptions={[5]}
                            checkboxSelection
                            hideFooter={true}
                            disableRowSelectionOnClick
                        />
                    </Box>
                </Box>
                {requestToGiveDetail &&
                    <Modal handleClose={handleClose} handleMarkComplete={handleModalComplete} open={open}
                           requestDetail={requestToGiveDetail}/>}
            </Stack>
        </Stack>
    )
}

export default ResponderPage;