INSERT INTO role (name)
VALUES ('REQUESTER'),
       ('RESPONDER'),
       ('DISPATCHER');

INSERT INTO nine_line_user (username, password, callsign)
VALUES ('Batman', '$2y$10$GcNE/C.wflm6JA69JF8GZe9YFKHkvhXEn9ympl2ti5Q7ryKnwEUxe',
        'BMan_4'), -- password is Password
       ('Superman', '$2y$10$GcNE/C.wflm6JA69JF8GZe9YFKHkvhXEn9ympl2ti5Q7ryKnwEUxe', 'BigS_3'),
       ('Ironman', '$2y$10$GcNE/C.wflm6JA69JF8GZe9YFKHkvhXEn9ympl2ti5Q7ryKnwEUxe', 'IRON_2'),
       ('Wonderwoman', '$2y$10$GcNE/C.wflm6JA69JF8GZe9YFKHkvhXEn9ympl2ti5Q7ryKnwEUxe', NULL),
       ('Darthvader', '$2y$10$GcNE/C.wflm6JA69JF8GZe9YFKHkvhXEn9ympl2ti5Q7ryKnwEUxe', NULL);

INSERT INTO nine_line_user_roles (nine_line_user_id, roles_id)
VALUES (1, 2),
       (2, 2),
       (3, 2),
       (4, 3),
       (5, 2),
       (5, 3);

INSERT INTO medevac_request(location, callsign, patientnumber, precedence, litterpatient,
                                               ambulatorypatient, security, nationality, nbc, status)
VALUES ('56J MS 60443 25375', '922929/Arrow/6', 1, 1, 1, 0, 'N', 1, 1, 'Pending'),
       ('56J MS 90443 12227', '222333/Arrow/6', 1, 1, 1, 0, 'N', 1, 1, 'Pending'),
       ('56J MS 20443 18666', '111222/Arrow/6', 1, 1, 1, 0, 'N', 1, 1, 'Pending');

INSERT INTO assignment(request_id, responder)
VALUES (1, 1),
       (2, 1),
       (3, 1);
