const fs = require("fs");
const {staffRoles} = require("../configs/config.json");

function isLongText(text) {
    return text.length >= 1900;
}

function detectEndOfWord(text, position, look_for_format){
    const slice=text.slice(position);
    if (!look_for_format) {
        for (const char of slice) {
            position += 1;
            if (char === ' ' || char === ',' || char === '\n') {
                return position;
            }
        }
    } else {
        let format_counter = 0;
        for (const char of slice) {
            if (char === '`') {
                format_counter += 1;
            }
            position += 1;
            if (format_counter === 3) {
                return position;
            }
        }
    }
}

function separateLongTextLocal(text, look_for_format) {
    if (isLongText(text)) {
        const separator = detectEndOfWord(text, 1900, look_for_format);
        const left = text.slice(0, -(text.length - separator));
        const right = text.slice(separator);
        return [left].concat(separateLongTextLocal(right));
    } else {
        return [text];
    }
}

function addSubcommands(parentCommand, hasAdminSubcommands) {
    let path = `./commands/subcommands/${parentCommand}/`;
    let files = fs.readdirSync(path, (err, tmp_files) => tmp_files.filter(file => file.contains(`${parentCommand}_`)));
    const commands = {};
    for (const file of files) {
        const name = file.split(`${parentCommand}_`)[1].slice(0, -3);
        commands[name] = require(`../commands/subcommands/${parentCommand}/` + file);
    }
    if (hasAdminSubcommands) {
        path = `./commands/subcommands/admin/${parentCommand}/`;
        files = fs.readdirSync(path, (err, tmp_files) => tmp_files.filter(file => file.contains(`${parentCommand}_`)));
        for (const file of files) {
            const name = file.split(`${parentCommand}_`)[1].slice(0, -3);
            commands[name] = require(`../commands/subcommands/admin/${parentCommand}/` + file);
        }
    }
    return commands;
}

function isStaffMember(interaction) {
    return staffRoles.some(role => interaction.member.roles.cache.has(role));
}

function createArmyUnitListString(army) {
    let unitString = "";
    for (let i = 0; i < army.units.length; i++) {
        let unit = army.units[i];
        let unitsAlive = `${unit.amountAlive}/${unit.count} `;
        let unitName = `${unit.unitType.unitName}`
        if(unit.unitType.unitName === undefined)
            unitName = `${unit.unitType}`
        unitString += unitsAlive + unitName + "\n";
    }

    return unitString;
}

function createUnpaidString(armies) {
    unpaidString = "";
    for(i=0; i < armies.length; i++) {
        army = armies[i];

        armyName = army.name;
        faction = army.faction.name;
        createdAt = army.createdAt.substring(0,10);
        console.log( `${createdAt}`)

        unpaidString += `Name: ${armyName} | Faction: ${faction} | Creation date: ${createdAt}\n`;
    }
    console.log(unpaidString)
    if(unpaidString === "") {
        unpaidString = "No armies unpaid";
    }
    return unpaidString;
}

module.exports = {

    separate_long_text(text, look_for_format = false) {
        return separateLongTextLocal(text, look_for_format);
    },
    capitalizeFirstLetters(text) {
        const arr_text = text.split(/[,.\s]+/);
        for (let i = 0; i < arr_text.length; i++) {
            arr_text[i] = arr_text[i].charAt(0).toUpperCase() + arr_text[i].slice(1);
        }
        return arr_text.join(" ");
    },
    addSubcommands: addSubcommands,
    isMemberStaff: isStaffMember,
    createArmyUnitListString: createArmyUnitListString,
    createUnpaidString: createUnpaidString
};