import React from 'react';
import { Link } from 'react-router-dom';
import PageTitle from '../PageTitle';
import './NotFound.css';

const PAGE_NOT_FOUND_TITLE = 'העמוד לא נמצא';
const NOT_FOUND_CODE = '404';
const HOME_LABEL = 'לדף הבית';

// Also used for an unknown room: the caller names what is missing and where to go back to.
const NotFound = ({ title = PAGE_NOT_FOUND_TITLE, backTo = '/', backLabel = HOME_LABEL }) => (
    <div className="container not-found">
        <PageTitle>{title}</PageTitle>
        <span className="not-found-stamp" aria-hidden="true">{NOT_FOUND_CODE}</span>
        <h1 className="heading-2">{title}</h1>
        <Link to={backTo} className="btn btn-primary">{backLabel}</Link>
    </div>
);

export default NotFound;
