(function() {
    'use strict';

    // Configuration
    const TARGET_PATH = '/api/tickets/{ticketId}';

    // Enum values from the backend
    const OPERATIONS = ['STATUS', 'PRIORITY', 'CATEGORY', 'ASSIGN_AGENT', 'ASSIGN_MANAGER', 'HOLD', 'RESUME', 'RESOLVE', 'REOPEN', 'WITHDRAW'];
    const TICKET_STATUS_VALUES = ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'NEED_EMPLOYEE_COMMUNICATION', 'RESOLVED', 'BREACHED', 'CLOSED', 'REOPENED', 'WITHDRAWN', 'CANCELLED'];
    const PRIORITY_VALUES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

    // Operation field configurations
    const OPERATION_FIELDS = {
        STATUS: [
            { name: 'status', label: 'New Status', type: 'select', options: TICKET_STATUS_VALUES, required: true }
        ],
        PRIORITY: [
            { name: 'priority', label: 'New Priority', type: 'select', options: PRIORITY_VALUES, required: true }
        ],
        CATEGORY: [
            { name: 'categoryId', label: 'Category ID', type: 'number', required: true },
            { name: 'subCategoryId', label: 'Sub Category ID', type: 'number', required: true }
        ],
        ASSIGN_AGENT: [
            { name: 'agentId', label: 'Agent ID', type: 'number', required: true }
        ],
        ASSIGN_MANAGER: [
            { name: 'assignedManagerId', label: 'Manager ID', type: 'number', required: true }
        ],
        HOLD: [
            { name: 'holdReason', label: 'Hold Reason', type: 'text', required: true }
        ],
        RESUME: [],
        RESOLVE: [
            { name: 'resolutionSummary', label: 'Resolution Summary', type: 'text', required: true }
        ],
        REOPEN: [
            { name: 'reason', label: 'Reason', type: 'text', required: true }
        ],
        WITHDRAW: [
            { name: 'withdrawalReason', label: 'Withdrawal Reason', type: 'text', required: true }
        ]
    };

    let customFormContainer = null;
    let originalRequestBodyEditor = null;

    // Wait for Swagger UI to fully load
    function waitForSwaggerUI() {
        if (typeof window.ui !== 'undefined' && window.ui.specSelectors) {
            // Wait a bit for the UI to render
            setTimeout(initCustomization, 1000);
        } else {
            setTimeout(waitForSwaggerUI, 100);
        }
    }

    // Initialize customization - single pass, no continuous scanning
    function initCustomization() {
        console.log('Initializing PATCH customization for:', TARGET_PATH);
        
        // Find the PATCH operation block
        const patchOperation = findPatchOperation();
        if (!patchOperation) {
            console.log('PATCH operation not found yet, will retry once');
            setTimeout(initCustomization, 2000);
            return;
        }

        console.log('Found PATCH operation, setting up customization');
        customizeEndpoint(patchOperation);
    }

    // Find the target PATCH operation
    function findPatchOperation() {
        const patchOperations = document.querySelectorAll('.opblock.patch');
        for (let i = 0; i < patchOperations.length; i++) {
            const pathElement = patchOperations[i].querySelector('.opblock-summary-path');
            if (pathElement && pathElement.textContent.includes(TARGET_PATH)) {
                return patchOperations[i];
            }
        }
        return null;
    }

    // Customize the endpoint
    function customizeEndpoint(operationBlock) {
        // Check if already customized
        if (operationBlock.dataset.patchCustomized === 'true') return;
        operationBlock.dataset.patchCustomized = 'true';

        // Add click handler to the try-out button
        const tryButton = operationBlock.querySelector('.try-out__btn');
        if (!tryButton) {
            console.log('Try-out button not found, retrying...');
            setTimeout(function() {
                customizeEndpoint(operationBlock);
            }, 500);
            return;
        }

        tryButton.addEventListener('click', function() {
            console.log('Try-out button clicked');
            // Wait for the request body editor to appear
            setTimeout(function() {
                const requestBodyEditor = operationBlock.querySelector('.opblock-body .microlight');
                if (requestBodyEditor && !requestBodyEditor.dataset.patchFormReplaced) {
                    requestBodyEditor.dataset.patchFormReplaced = 'true';
                    replaceRequestBodyEditor(requestBodyEditor, operationBlock);
                }
            }, 300);
        });
    }

    // Replace the request body editor with custom form
    function replaceRequestBodyEditor(editor, operationBlock) {
        console.log('Replacing request body editor with custom form');
        originalRequestBodyEditor = editor;
        
        const parentContainer = editor.closest('.opblock-section-request-body');
        if (!parentContainer) return;
        
        customFormContainer = document.createElement('div');
        customFormContainer.className = 'swagger-ticket-patch-form';
        customFormContainer.style.padding = '16px';
        customFormContainer.style.background = '#f8f8f8';
        customFormContainer.style.borderRadius = '4px';
        customFormContainer.style.marginBottom = '16px';

        buildForm(customFormContainer, operationBlock);

        editor.style.display = 'none';
        parentContainer.insertBefore(customFormContainer, editor);
    }

    // Build the custom form
    function buildForm(container, operationBlock) {
        // Operation selector
        const operationLabel = document.createElement('label');
        operationLabel.textContent = 'Operation:';
        operationLabel.style.display = 'block';
        operationLabel.style.fontWeight = 'bold';
        operationLabel.style.marginBottom = '8px';
        container.appendChild(operationLabel);

        const operationSelect = document.createElement('select');
        operationSelect.id = 'swagger-patch-operation';
        operationSelect.style.width = '100%';
        operationSelect.style.padding = '8px';
        operationSelect.style.marginBottom = '16px';
        operationSelect.style.border = '1px solid #ccc';
        operationSelect.style.borderRadius = '4px';
        operationSelect.style.fontSize = '14px';
        
        OPERATIONS.forEach(function(op) {
            const option = document.createElement('option');
            option.value = op;
            option.textContent = op;
            operationSelect.appendChild(option);
        });
        
        container.appendChild(operationSelect);

        // Fields container
        const fieldsContainer = document.createElement('div');
        fieldsContainer.id = 'swagger-patch-fields';
        container.appendChild(fieldsContainer);

        // Submit button
        const submitBtn = document.createElement('button');
        submitBtn.textContent = 'PATCH';
        submitBtn.type = 'button';
        submitBtn.style.background = '#61affe';
        submitBtn.style.color = 'white';
        submitBtn.style.border = 'none';
        submitBtn.style.padding = '10px 20px';
        submitBtn.style.borderRadius = '4px';
        submitBtn.style.cursor = 'pointer';
        submitBtn.style.fontSize = '14px';
        submitBtn.style.fontWeight = 'bold';
        submitBtn.style.marginTop = '16px';
        container.appendChild(submitBtn);

        // Handle operation change
        operationSelect.addEventListener('change', function() {
            updateFields(fieldsContainer, operationSelect.value);
        });

        // Handle submit
        submitBtn.addEventListener('click', function() {
            handleSubmit(operationBlock, operationSelect.value, fieldsContainer);
        });

        // Initialize with first operation
        updateFields(fieldsContainer, operationSelect.value);
    }

    // Update fields based on selected operation
    function updateFields(container, operation) {
        container.innerHTML = '';
        
        const fields = OPERATION_FIELDS[operation] || [];
        
        fields.forEach(function(field) {
            const label = document.createElement('label');
            label.textContent = field.label + (field.required ? ' *' : '');
            label.style.display = 'block';
            label.style.fontWeight = 'bold';
            label.style.marginBottom = '4px';
            label.style.marginTop = '12px';
            container.appendChild(label);

            let input;
            if (field.type === 'select') {
                input = document.createElement('select');
                input.style.width = '100%';
                input.style.padding = '8px';
                input.style.border = '1px solid #ccc';
                input.style.borderRadius = '4px';
                input.style.fontSize = '14px';
                field.options.forEach(function(opt) {
                    const option = document.createElement('option');
                    option.value = opt;
                    option.textContent = opt;
                    input.appendChild(option);
                });
            } else if (field.type === 'number') {
                input = document.createElement('input');
                input.type = 'number';
                input.style.width = '100%';
                input.style.padding = '8px';
                input.style.border = '1px solid #ccc';
                input.style.borderRadius = '4px';
                input.style.fontSize = '14px';
            } else {
                input = document.createElement('input');
                input.type = 'text';
                input.style.width = '100%';
                input.style.padding = '8px';
                input.style.border = '1px solid #ccc';
                input.style.borderRadius = '4px';
                input.style.fontSize = '14px';
            }
            
            input.dataset.fieldName = field.name;
            input.dataset.required = field.required;
            container.appendChild(input);
        });

        if (fields.length === 0) {
            const info = document.createElement('div');
            info.textContent = 'No additional fields required for this operation.';
            info.style.color = '#666';
            info.style.fontStyle = 'italic';
            info.style.marginTop = '8px';
            container.appendChild(info);
        }
    }

    // Handle form submission
    function handleSubmit(operationBlock, operation, fieldsContainer) {
        const inputs = fieldsContainer.querySelectorAll('input, select');
        const data = {};
        let valid = true;

        inputs.forEach(function(input) {
            const fieldName = input.dataset.fieldName;
            const required = input.dataset.required === 'true';
            const value = input.value.trim();

            if (required && !value) {
                input.style.borderColor = 'red';
                valid = false;
            } else {
                input.style.borderColor = '#ccc';
                if (value) {
                    if (input.type === 'number') {
                        data[fieldName] = parseInt(value, 10);
                    } else {
                        data[fieldName] = value;
                    }
                }
            }
        });

        if (!valid) {
            alert('Please fill in all required fields.');
            return;
        }

        const updateDTO = {
            operation: operation,
            data: data
        };

        console.log('Submitting:', updateDTO);

        const textarea = originalRequestBodyEditor;
        if (textarea) {
            textarea.value = JSON.stringify(updateDTO, null, 2);
            textarea.style.display = 'block';
            
            const event = new Event('input', { bubbles: true });
            textarea.dispatchEvent(event);
        }

        const executeBtn = operationBlock.querySelector('.execute');
        if (executeBtn) {
            executeBtn.click();
        }
    }

    // Start
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', waitForSwaggerUI);
    } else {
        waitForSwaggerUI();
    }
})();
